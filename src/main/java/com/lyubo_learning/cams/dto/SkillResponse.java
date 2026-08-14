package com.lyubo_learning.cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "A skill from the fixed ESCO-sourced vocabulary")
public class SkillResponse {

    @Schema(description = "Unique identifier of the skill", example = "1")
    private Long id;

    @Schema(description = "Name of the skill", example = "manage project resources")
    private String name;
}
