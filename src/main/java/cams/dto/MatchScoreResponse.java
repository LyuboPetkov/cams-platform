package cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "The authenticated candidate's semantic match against a single listing")
public class MatchScoreResponse {

    @Schema(description = "Cosine similarity between the caller's profile embedding and the listing's, " +
            "roughly in [0, 1]. Null when the caller is not a candidate, their profile has no embedding " +
            "yet, or the listing has none - the listing page just omits the badge in that case.",
            example = "0.82", nullable = true)
    private Double score;
}
