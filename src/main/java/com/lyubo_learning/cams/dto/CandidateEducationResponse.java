package com.lyubo_learning.cams.dto;

import com.lyubo_learning.cams.entity.EducationLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "A single education entry")
public class CandidateEducationResponse {

    @Schema(description = "Unique identifier of the education entry", example = "1")
    private Long id;

    @Schema(description = "Institution name", example = "Sofia University")
    private String institutionName;

    @Schema(description = "Level of education")
    private EducationLevel level;

    @Schema(description = "Start date; day is not meaningful, only month/year", example = "2020-09-01")
    private LocalDate startDate;

    @Schema(description = "End date; null means ongoing/expected", example = "2024-06-01", nullable = true)
    private LocalDate endDate;

}
