package cams.service;

import cams.dto.auth.AuthResponse;
import cams.dto.auth.LoginRequest;
import cams.dto.auth.RegisterRequest;
import cams.dto.auth.RegisterResponse;
import cams.entity.AccountStatus;
import cams.entity.CandidateProfile;
import cams.entity.EmployerRequest;
import cams.entity.EmployerRequestStatus;
import cams.entity.Role;
import cams.entity.User;
import cams.exception.AccountNotActiveException;
import cams.exception.EmailAlreadyExistsException;
import cams.exception.InvalidCredentialsException;
import cams.exception.InvalidRegistrationException;
import cams.exception.InvalidRequestStateException;
import cams.exception.ResourceNotFoundException;
import cams.repository.CandidateProfileRepository;
import cams.repository.EmployerRequestRepository;
import cams.repository.UserRepository;
import cams.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmployerRequestRepository employerRequestRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CompanyService companyService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                    "Email already in use: " + request.getEmail()
            );
        }

        boolean isEmployer = request.getCompanyName() != null && !request.getCompanyName().isBlank();

        if (!isEmployer && (request.getFullName() == null || request.getFullName().isBlank())) {
            throw new InvalidRegistrationException("Full name is required");
        }

        return isEmployer ? registerEmployer(request) : registerCandidate(request);
    }

    private RegisterResponse registerCandidate(RegisterRequest request) {
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.CANDIDATE)
                .status(AccountStatus.ACTIVE)
                .build();

        userRepository.save(user);

        // Every candidate account gets a profile up front so nothing downstream
        // has to handle "no profile yet". Employer registrants never get one.
        candidateProfileRepository.save(CandidateProfile.builder().user(user).build());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return new RegisterResponse(token, user.getEmail(), user.getFullName(), user.getRole(),
                resolvePictureUrl(user), user.getStatus());
    }

    // No CandidateProfile, no JWT - the account stays PENDING_APPROVAL and
    // unusable until an admin approves the employer request.
    private RegisterResponse registerEmployer(RegisterRequest request) {
        String normalizedCompanyName = companyService.normalizeName(request.getCompanyName());

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(normalizedCompanyName)
                .role(Role.CANDIDATE)
                .status(AccountStatus.PENDING_APPROVAL)
                .build();

        userRepository.save(user);

        createEmployerRequest(user, request);

        return new RegisterResponse(null, user.getEmail(), user.getFullName(), null, null, user.getStatus());
    }

    // CANDIDATE -> profile picture, EMPLOYER -> company logo, ADMIN -> null.
    private String resolvePictureUrl(User user) {
        if (user.getRole() == Role.CANDIDATE) {
            return candidateProfileRepository.findByUserId(user.getId())
                    .map(CandidateProfile::getPictureUrl)
                    .orElse(null);
        }
        if (user.getRole() == Role.EMPLOYER) {
            return user.getCompany() != null ? user.getCompany().getLogoUrl() : null;
        }
        return null;
    }

    // Never trips via register() itself - guards a future "existing candidate
    // becomes an employer" path that would reuse this method.
    private void createEmployerRequest(User user, RegisterRequest request) {
        boolean alreadyPending = employerRequestRepository
                .findFirstByUserIdOrderByRequestedAtDesc(user.getId())
                .map(existing -> existing.getStatus() == EmployerRequestStatus.PENDING)
                .orElse(false);

        if (alreadyPending) {
            throw new InvalidRequestStateException("An employer request is already pending for this user");
        }

        // Checked at submission time so a duplicate company name is rejected immediately.
        companyService.ensureNameAvailableForNewRequest(companyService.normalizeName(request.getCompanyName()));

        EmployerRequest employerRequest = EmployerRequest.builder()
                .user(user)
                .companyName(request.getCompanyName())
                .companyDescription(request.getCompanyDescription())
                .companyWebsite(request.getCompanyWebsite())
                .companyLocation(request.getCompanyLocation())
                .status(EmployerRequestStatus.PENDING)
                .requestedAt(Instant.now())
                .build();

        employerRequestRepository.save(employerRequest);
    }

    // @Transactional so the LAZY user.getCompany() in resolvePictureUrl() stays
    // resolvable - an EMPLOYER login would otherwise throw LazyInitializationException.
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Ïnvalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (user.getStatus() == AccountStatus.PENDING_APPROVAL) {
            throw new AccountNotActiveException("Your employer registration is still under review.");
        }
        if (user.getStatus() == AccountStatus.REJECTED) {
            String reason = employerRequestRepository.findFirstByUserIdOrderByRequestedAtDesc(user.getId())
                    .map(EmployerRequest::getRejectionReason)
                    .orElse(null);
            String suffix;
            if (reason != null && !reason.isBlank()) {
                String trimmedReason = reason.trim();
                boolean endsWithPunctuation = trimmedReason.endsWith(".") || trimmedReason.endsWith("!")
                        || trimmedReason.endsWith("?");
                suffix = ": " + trimmedReason + (endsWithPunctuation ? "" : ".");
            } else {
                suffix = ".";
            }
            throw new AccountNotActiveException(
                    "Your employer registration was rejected" + suffix + " You can resubmit your application.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return new AuthResponse(token, user.getEmail(), user.getFullName(), user.getRole(),
                resolvePictureUrl(user));
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public void updateMyFullName(String fullName) {
        User user = getAuthenticatedUser();
        user.setFullName(fullName);
        userRepository.save(user);
    }
}
