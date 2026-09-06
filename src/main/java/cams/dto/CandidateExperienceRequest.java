package cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Schema(description = "A single work experience entry")
public class CandidateExperienceRequest {

    @NotBlank
    @Schema(description = "Employer the role was held at", example = "Acme Software")
    private String companyName;

    @NotBlank
    @Schema(description = "Role title", example = "Backend Engineer")
    private String roleTitle;

    @NotNull
    @Schema(description = "Start date; day is ignored, only month/year matter", example = "2020-09-01")
    private LocalDate startDate;

    @Schema(description = "End date; null means ongoing. Day is ignored, only month/year matter.",
            example = "2024-06-01", nullable = true)
    private LocalDate endDate;

    @Schema(description = "Free-text description of the role", example = "Built and maintained payment services.",
            nullable = true)
    private String description;

}
