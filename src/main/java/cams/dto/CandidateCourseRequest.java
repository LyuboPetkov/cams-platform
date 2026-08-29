package cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Schema(description = "A single course/certificate entry")
public class CandidateCourseRequest {

    @NotBlank
    @Schema(description = "Course/certificate title", example = "AWS Certified Solutions Architect")
    private String name;

    @NotBlank
    @Schema(description = "Issuing organization", example = "Amazon Web Services")
    private String issuingOrganization;

    @Schema(description = "Completion date; null means ongoing/no date given", example = "2023-05-01",
            nullable = true)
    private LocalDate completionDate;

    @URL(message = "Credential URL must be a valid URL")
    @Schema(description = "Optional link to the credential", nullable = true)
    private String credentialUrl;

}
