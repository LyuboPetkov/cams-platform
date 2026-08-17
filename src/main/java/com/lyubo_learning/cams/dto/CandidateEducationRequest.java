package com.lyubo_learning.cams.dto;

import com.lyubo_learning.cams.entity.EducationLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Schema(description = "A single education entry")
public class CandidateEducationRequest {

    @NotBlank
    @Schema(description = "Institution name", example = "Sofia University")
    private String institutionName;

    @NotNull
    @Schema(description = "Level of education")
    private EducationLevel level;

    @NotNull
    @Schema(description = "Start date; day is ignored, only month/year matter", example = "2020-09-01")
    private LocalDate startDate;

    @Schema(description = "End date; null means ongoing/expected. Day is ignored, only month/year matter.",
            example = "2024-06-01", nullable = true)
    private LocalDate endDate;

}
