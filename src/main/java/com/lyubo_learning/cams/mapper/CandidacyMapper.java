package com.lyubo_learning.cams.mapper;

import com.lyubo_learning.cams.dto.CandidacyResponse;
import com.lyubo_learning.cams.entity.Candidacy;
import org.springframework.stereotype.Component;

@Component
public class CandidacyMapper {

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
}
