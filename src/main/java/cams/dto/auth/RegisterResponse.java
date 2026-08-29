package cams.dto.auth;

import cams.entity.AccountStatus;
import cams.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

// Candidate registration fills every field. Employer registration returns
// token/role/pictureUrl null and status = PENDING_APPROVAL - not usable yet.
@Getter
@AllArgsConstructor
public class RegisterResponse {

    private String token;
    private String email;
    private String fullName;
    private Role role;
    private String pictureUrl;
    private AccountStatus status;
}
