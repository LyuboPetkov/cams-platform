package cams.dto;

import cams.entity.CandidacyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// One DTO serves both the candidate's own list and the employer's per-listing
// view — no field here should differ by viewer.
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "A candidate's application to a job listing")
public class CandidacyResponse {

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
}
