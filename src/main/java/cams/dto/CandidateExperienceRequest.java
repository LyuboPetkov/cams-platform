package cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "A single work experience entry")
public class CandidateExperienceRequest {

    @NotBlank
    @Schema(description = "Role title", example = "Backend Engineer")
    private String roleTitle;

    @NotNull
    @Min(0)
    @Schema(description = "Years of experience in this role", example = "3")
    private Integer yearsOfExperience;

    @Schema(description = "Free-text description of the role", example = "Built and maintained payment services.",
            nullable = true)
    private String description;

}
