package cams.config;

import cams.entity.AccountStatus;
import cams.entity.Role;
import cams.entity.User;
import cams.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Runs once at startup so the first ADMIN account needs no direct DB access;
// unset ADMIN_EMAIL/ADMIN_PASSWORD just skips it.
@Slf4j
@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrapRunner(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${application.admin-bootstrap.email:}") String adminEmail,
                                 @Value("${application.admin-bootstrap.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("Admin bootstrap skipped: ADMIN_EMAIL/ADMIN_PASSWORD not set");
            return;
        }

        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin bootstrap skipped: a user with email {} already exists", adminEmail);
            return;
        }

        // No CandidateProfile - that invariant is candidates-only.
        User admin = User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .fullName("Admin")
                .role(Role.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();

        userRepository.save(admin);
        log.info("Admin bootstrap: created ADMIN user {}", adminEmail);
    }
}
