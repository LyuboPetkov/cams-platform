package cams.dto;

import cams.entity.CandidacyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// The employer's view of a candidacy. Deliberately a separate type from
// CandidacyResponse rather than extra fields on it: the moment candidate
// identity is added, "no field here should differ by viewer" stops being true,
// and a candidate reading their own list has no use for their own name or email.
// Two DTOs, one per viewer, beats one DTO with fields that are null by audience.
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
}
