package com.lyubo_learning.cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// A thin wrapper around CandidateSummary rather than a flat copy of its
// fields, the mirror image of JobListingMatchResponse.
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "A candidate matched to one of the employer's listings, ranked by embedding similarity")
public class CandidateMatchResponse {

    @Schema(description = "The matched candidate")
    private CandidateSummary candidate;

    @Schema(description = "Cosine similarity between the listing's embedding and the candidate's profile embedding, in [-1, 1] (1 - cosine distance), not a percentage", example = "0.8421")
    private double similarityScore;
}
