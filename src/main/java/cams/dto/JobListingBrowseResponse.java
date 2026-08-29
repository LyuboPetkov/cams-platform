package cams.dto;

import cams.entity.CandidacyStatus;
import cams.entity.JobLevel;
import cams.entity.JobListingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

// JobListingResponse minus postedByUserId, which would leak employer staff user
// ids to every browsing candidate. Still carries the full description and skills.
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "A job listing as seen by a candidate browsing the platform")
public class JobListingBrowseResponse {

    @Schema(description = "Unique identifier of the listing", example = "1")
    private Long id;

    @Schema(description = "Job title", example = "Backend developer")
    private String title;

    @Schema(description = "Full description of the role", example = "You will build and maintain our Spring services.")
    private String description;

    @Schema(description = "The company the listing belongs to")
    private CompanyResponse company;

    @Schema(description = "Where the job is based; set per listing, not copied from the company", example = "Sofia, Bulgaria", nullable = true)
    private String location;

    @Schema(description = "Whether the role can be done remotely")
    private Boolean remote;

    @Schema(description = "Seniority level of the role")
    private JobLevel level;

    @Schema(description = "Always OPEN on the browse endpoint specifically; the candidate-view-by-id " +
            "endpoint can return an ARCHIVED listing the caller has already applied to")
    private JobListingStatus status;

    @Schema(description = "Skills required for this listing")
    private List<SkillResponse> skills;

    @Schema(description = "Timestamp when the listing was created")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the listing was last updated")
    private LocalDateTime updatedAt;

    @Schema(description = "The authenticated candidate's own candidacy status against this listing, " +
            "null if they haven't applied or the caller isn't a candidate", nullable = true)
    private CandidacyStatus myCandidacyStatus;
}
