package cams.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(description = "Email address used to log in", example = "john@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "Password — minimum 8 characters", example = "securepass123")
    private String password;

    @NotBlank(message = "Full name is required")
    @Schema(description = "Name of the applicant", example = "John Smith")
    private String fullName;

    @Schema(description = "Company name — providing this submits an employer request alongside registration", example = "Acme Inc.")
    private String companyName;

    @Schema(description = "Optional company description", example = "We build widgets.")
    private String companyDescription;

    @Schema(description = "Optional company website", example = "https://acme.example.com")
    private String companyWebsite;

    @Schema(description = "Optional company location", example = "Sofia, Bulgaria")
    private String companyLocation;
}
