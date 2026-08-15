package com.lyubo_learning.cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "The listing's full required skill set. Replaces the current selection rather than adding to it.")
public class JobListingSkillsUpdateRequest {

    @NotNull(message = "skillIds is required")
    @Schema(description = "Ids of the skills to require; an empty list clears the selection", example = "[12, 3481, 907]")
    private List<Long> skillIds;
}
