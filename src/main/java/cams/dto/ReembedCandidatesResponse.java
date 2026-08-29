package cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Result of an admin candidate re-embed run")
public class ReembedCandidatesResponse {

    @Schema(description = "Number of candidate profiles queued for re-embedding", example = "42")
    private int queued;

}
