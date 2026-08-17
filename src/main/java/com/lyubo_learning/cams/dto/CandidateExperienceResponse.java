package com.lyubo_learning.cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "A single work experience entry")
public class CandidateExperienceResponse {

    @Schema(description = "Unique identifier of the experience entry", example = "1")
    private Long id;

    @Schema(description = "Role title", example = "Backend Engineer")
    private String roleTitle;

    @Schema(description = "Years of experience in this role", example = "3")
    private Integer yearsOfExperience;

    @Schema(description = "Free-text description of the role", nullable = true)
    private String description;

}
