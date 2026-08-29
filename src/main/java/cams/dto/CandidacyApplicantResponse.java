package cams.dto;

import cams.entity.CandidacyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// The employer's view of a candidacy - a separate type from CandidacyResponse
// (the candidate's view), not one DTO with reader-dependent null fields.
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "A candidacy as shown to the employer reviewing applicants for a listing")
public class CandidacyApplicantResponse {

    @Schema(description = "Unique identifier of the candidacy", example = "1")
    private Long id;

    @Schema(description = "Id of the listing applied to", example = "12")
    private Long jobListingId;

    @Schema(description = "Title of the listing applied to", example = "Backend developer")
    private String jobListingTitle;

    @Schema(description = "Name of the company that posted the listing", example = "Acme Ltd")
    private String companyName;

    @Schema(description = "Current status of the candidacy")
    private CandidacyStatus status;

    @Schema(description = "When the candidate applied")
    private LocalDateTime appliedAt;

    @Schema(description = "Who applied")
    private CandidateSummary candidate;

    @Schema(description = "Cosine similarity between the listing's embedding and the candidate's profile embedding, in [-1, 1] (1 - cosine distance), not a percentage. Null if the listing or the candidate's profile has no embedding yet", example = "0.8421", nullable = true)
    private Double matchScore;

    @Schema(description = "True if the listing isn't remote and the candidate's profile location doesn't appear to overlap with the listing's location. A soft, informational warning - it does not affect matchScore or exclude the applicant", example = "false")
    private boolean locationMismatch;

    @Schema(description = "True if the listing is remote but the candidate's profile explicitly states they are not open to remote work. Independent of locationMismatch - a stated-preference note, not a location conflict", example = "false")
    private boolean openToRemoteMismatch;
}
