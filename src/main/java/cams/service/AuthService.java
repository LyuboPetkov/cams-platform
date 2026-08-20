package cams.service;

import cams.dto.auth.AuthResponse;
import cams.dto.auth.LoginRequest;
import cams.dto.auth.RegisterRequest;
import cams.entity.CandidateProfile;
import cams.entity.EmployerRequest;
import cams.entity.EmployerRequestStatus;
import cams.entity.Role;
import cams.entity.User;
import cams.exception.EmailAlreadyExistsException;
import cams.exception.InvalidCredentialsException;
import cams.exception.InvalidRequestStateException;
import cams.repository.CandidateProfileRepository;
import cams.repository.EmployerRequestRepository;
import cams.repository.UserRepository;
import cams.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                    "Email already in use: " + request.getEmail()
            );
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.CANDIDATE)
                .build();

        userRepository.save(user);

        // Every account gets a profile up front, so nothing downstream has to treat
        // "no profile yet" as a case — unlike Company, where absence is the norm.
        candidateProfileRepository.save(CandidateProfile.builder().user(user).build());

        if (request.getCompanyName() != null && !request.getCompanyName().isBlank()) {
            createEmployerRequest(user, request);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return new AuthResponse(token, user.getEmail(), user.getFullName(), user.getRole(),
                hasPendingEmployerRequest(user));

    }

    // Same lookup createEmployerRequest() already does to guard against a
    // duplicate PENDING request, just read-only here.
    private boolean hasPendingEmployerRequest(User user) {
        return employerRequestRepository.findFirstByUserIdOrderByRequestedAtDesc(user.getId())
                .map(request -> request.getStatus() == EmployerRequestStatus.PENDING)
                .orElse(false);
    }

    // A brand-new user can never already have a PENDING request, so this never trips
    // via register() itself — it guards correctness if this creation path is ever
    // reused by a future "existing candidate applies to become an employer" endpoint.
    private void createEmployerRequest(User user, RegisterRequest request) {
        boolean alreadyPending = employerRequestRepository
                .findFirstByUserIdOrderByRequestedAtDesc(user.getId())
                .map(existing -> existing.getStatus() == EmployerRequestStatus.PENDING)
                .orElse(false);

        if (alreadyPending) {
            throw new InvalidRequestStateException("An employer request is already pending for this user");
        }

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

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        return new AuthResponse(token, user.getEmail(), user.getFullName(), user.getRole(),
                hasPendingEmployerRequest(user));
    }
}
