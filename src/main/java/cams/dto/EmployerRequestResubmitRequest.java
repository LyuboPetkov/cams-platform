package cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Getter
@NoArgsConstructor
@Schema(description = "Payload for resubmitting a rejected employer request. The account can't log in yet, " +
        "so email/password re-identify the applicant instead of a JWT.")
public class EmployerRequestResubmitRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(description = "Email of the rejected account", example = "john@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(description = "Password of the rejected account", example = "securepass123")
    private String password;

    @NotBlank(message = "Company name is required")
    @Schema(description = "Company name", example = "Acme Inc.")
    private String companyName;

    @Schema(description = "Optional company description", example = "We build widgets.")
    private String companyDescription;

    @URL(message = "Website must be a valid URL")
    @Size(max = 500, message = "Website must be at most 500 characters")
    @Schema(description = "Optional company website", example = "https://acme.example.com")
    private String companyWebsite;

    @Schema(description = "Optional company location", example = "Sofia, Bulgaria")
    private String companyLocation;
}
