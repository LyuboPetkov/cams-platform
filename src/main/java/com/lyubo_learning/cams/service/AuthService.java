package com.lyubo_learning.cams.service;

import com.lyubo_learning.cams.dto.auth.AuthResponse;
import com.lyubo_learning.cams.dto.auth.LoginRequest;
import com.lyubo_learning.cams.dto.auth.RegisterRequest;
import com.lyubo_learning.cams.entity.EmployerRequest;
import com.lyubo_learning.cams.entity.EmployerRequestStatus;
import com.lyubo_learning.cams.entity.Role;
import com.lyubo_learning.cams.entity.User;
import com.lyubo_learning.cams.exception.EmailAlreadyExistsException;
import com.lyubo_learning.cams.exception.InvalidCredentialsException;
import com.lyubo_learning.cams.exception.InvalidRequestStateException;
import com.lyubo_learning.cams.repository.EmployerRequestRepository;
import com.lyubo_learning.cams.repository.UserRepository;
import com.lyubo_learning.cams.security.JwtUtil;
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

        if (request.getCompanyName() != null && !request.getCompanyName().isBlank()) {
            createEmployerRequest(user, request);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return new AuthResponse(token, user.getEmail(), user.getFullName());

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

        return new AuthResponse(token, user.getEmail(), user.getFullName());
    }
}
