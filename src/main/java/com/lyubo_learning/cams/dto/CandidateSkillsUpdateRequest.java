package com.lyubo_learning.cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "The candidate's full desired skill set. Replaces the current selection rather than adding to it.")
public class CandidateSkillsUpdateRequest {

    @NotNull(message = "skillIds is required")
    @Schema(description = "Ids of the skills to attach; an empty list clears the selection", example = "[12, 3481, 907]")
    private List<Long> skillIds;
}
