package cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "A single work experience entry")
public class CandidateExperienceResponse {

    @Schema(description = "Unique identifier of the experience entry", example = "1")
    private Long id;

    @Schema(description = "Role title", example = "Backend Engineer")
    private String roleTitle;

    @Schema(description = "Start date; day is not meaningful, only month/year", example = "2020-09-01")
    private LocalDate startDate;

    @Schema(description = "End date; null means ongoing", example = "2024-06-01", nullable = true)
    private LocalDate endDate;

    @Schema(description = "Free-text description of the role", nullable = true)
    private String description;

}
