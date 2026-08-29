package cams.service;

import cams.entity.Role;
import cams.entity.User;
import cams.exception.ResourceNotFoundException;
import cams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    // Promoting an existing EMPLOYER leaves their company association untouched.
    @Transactional
    public void promoteToAdmin(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        user.setRole(Role.ADMIN);
        userRepository.save(user);
    }
}
