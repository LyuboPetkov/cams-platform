package com.lyubo_learning.cams.dto.auth;

import com.lyubo_learning.cams.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String email;
    private String fullName;
    private Role role;
    private boolean hasPendingEmployerRequest;
}
