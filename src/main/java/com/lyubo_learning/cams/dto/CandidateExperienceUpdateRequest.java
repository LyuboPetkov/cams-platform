package com.lyubo_learning.cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "The candidate's full experience list. Replaces the current entries rather than adding to them.")
public class CandidateExperienceUpdateRequest {

    @NotNull(message = "experiences is required")
    @Valid
    @Schema(description = "The full set of experience entries; an empty list clears it")
    private List<CandidateExperienceRequest> experiences;

}
