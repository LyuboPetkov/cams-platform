package cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

// What an employer needs to shortlist, no more (no free-text description).
// headline and skills are optional - a candidate can apply with no profile.
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "The applying candidate, as shown to the employer reviewing a listing")
public class CandidateSummary {

    @Schema(description = "Full name of the candidate", example = "Jane Doe")
    private String name;

    @Schema(description = "Email address of the candidate", example = "jane@example.com")
    private String email;

    @Schema(description = "Candidate's headline, if they have filled one in", example = "Backend developer", nullable = true)
    private String headline;

    @Schema(description = "URL of the candidate's profile picture, if they have set one", nullable = true)
    private String pictureUrl;

    @Schema(description = "Skills the candidate selected; empty if they have none or no profile")
    private List<SkillResponse> skills;
}
