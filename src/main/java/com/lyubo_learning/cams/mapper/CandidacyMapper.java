package com.lyubo_learning.cams.mapper;

import com.lyubo_learning.cams.dto.CandidacyApplicantResponse;
import com.lyubo_learning.cams.dto.CandidacyResponse;
import com.lyubo_learning.cams.dto.CandidateSummary;
import com.lyubo_learning.cams.entity.Candidacy;
import com.lyubo_learning.cams.entity.CandidateProfile;
import com.lyubo_learning.cams.entity.Skill;
import com.lyubo_learning.cams.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CandidacyMapper {

    private final SkillMapper skillMapper;

    // Reads through jobListing to its company, so it must run inside the
    // service's transaction — open-in-view is false. The repository queries
    // feeding the list reads fetch-join both to keep that from N+1ing.
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

    // The employer-facing shape. Profile and skills are passed in already
    // batch-loaded by the service rather than looked up per row here, and both
    // are optional: a candidate can apply without ever filling in a profile, so
    // profile may be null and skills may be empty.
    public CandidacyApplicantResponse toApplicantResponse(Candidacy entity,
                                                          CandidateProfile profile,
                                                          List<Skill> skills) {
        User candidate = entity.getCandidate();

        return CandidacyApplicantResponse.builder()
                .id(entity.getId())
                .jobListingId(entity.getJobListing().getId())
                .jobListingTitle(entity.getJobListing().getTitle())
                .companyName(entity.getJobListing().getCompany().getName())
                .status(entity.getStatus())
                .appliedAt(entity.getAppliedAt())
                .candidate(CandidateSummary.builder()
                        .name(candidate.getFullName())
                        .email(candidate.getEmail())
                        .headline(profile == null ? null : profile.getHeadline())
                        .skills(skills.stream().map(skillMapper::toResponse).toList())
                        .build())
                .build();
    }
}
