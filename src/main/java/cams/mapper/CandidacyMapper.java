package cams.mapper;

import cams.dto.CandidacyApplicantResponse;
import cams.dto.CandidacyResponse;
import cams.dto.CandidateSummary;
import cams.entity.Candidacy;
import cams.entity.CandidateProfile;
import cams.entity.Skill;
import cams.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CandidacyMapper {

    private final SkillMapper skillMapper;

    // Reads through jobListing to its company, so it must run inside the service's
    // transaction (open-in-view is false); the feeding queries fetch-join both.
    public CandidacyResponse toResponse(Candidacy entity) {
        return CandidacyResponse.builder()
                .id(entity.getId())
                .jobListingId(entity.getJobListing().getId())
                .jobListingTitle(entity.getJobListing().getTitle())
                .companyName(entity.getJobListing().getCompany().getName())
                .status(entity.getStatus())
                .appliedAt(entity.getAppliedAt())
                .build();
    }

    // Employer-facing shape. profile/skills are batch-loaded by the service
    // (profile may be null, skills empty); the score and mismatch flags are
    // computed there and passed straight through.
    public CandidacyApplicantResponse toApplicantResponse(Candidacy entity,
                                                          CandidateProfile profile,
                                                          List<Skill> skills,
                                                          Double matchScore,
                                                          boolean locationMismatch,
                                                          boolean openToRemoteMismatch) {
        return CandidacyApplicantResponse.builder()
                .id(entity.getId())
                .jobListingId(entity.getJobListing().getId())
                .jobListingTitle(entity.getJobListing().getTitle())
                .companyName(entity.getJobListing().getCompany().getName())
                .status(entity.getStatus())
                .appliedAt(entity.getAppliedAt())
                .candidate(toCandidateSummary(entity.getCandidate(), profile, skills))
                .matchScore(matchScore)
                .locationMismatch(locationMismatch)
                .openToRemoteMismatch(openToRemoteMismatch)
                .build();
    }

    // Shared with MatchingService, which has no Candidacy to build a full
    // CandidacyApplicantResponse around - it only needs the identity block.
    public CandidateSummary toCandidateSummary(User candidate, CandidateProfile profile, List<Skill> skills) {
        return CandidateSummary.builder()
                .name(candidate.getFullName())
                .email(candidate.getEmail())
                .headline(profile == null ? null : profile.getHeadline())
                .pictureUrl(profile == null ? null : profile.getPictureUrl())
                .skills(skills.stream().map(skillMapper::toResponse).toList())
                .build();
    }
}
