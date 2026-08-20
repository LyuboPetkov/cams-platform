package cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// A thin wrapper around the existing browse response rather than a flat copy
// of its fields: the field-copying is already done by
// JobListingMapper.toBrowseResponse(), this only adds the score on top.
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
