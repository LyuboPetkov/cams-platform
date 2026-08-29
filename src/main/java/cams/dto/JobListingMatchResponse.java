package cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// Thin wrapper over JobListingBrowseResponse that only adds the score -
// JobListingMapper.toBrowseResponse() already does the field-copying.
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "A job listing matched to the candidate's own profile, ranked by embedding similarity")
public class JobListingMatchResponse {

    @Schema(description = "The matched listing")
    private JobListingBrowseResponse listing;

    @Schema(description = "Cosine similarity between the candidate's profile embedding and the listing's embedding, in [-1, 1] (1 - cosine distance), not a percentage", example = "0.8421")
    private double similarityScore;
}
