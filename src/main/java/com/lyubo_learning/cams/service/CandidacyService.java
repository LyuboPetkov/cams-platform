package com.lyubo_learning.cams.service;

import com.lyubo_learning.cams.dto.CandidacyResponse;
import com.lyubo_learning.cams.entity.Candidacy;
import com.lyubo_learning.cams.entity.CandidacyStatus;
import com.lyubo_learning.cams.entity.JobListing;
import com.lyubo_learning.cams.entity.JobListingStatus;
import com.lyubo_learning.cams.entity.User;
import com.lyubo_learning.cams.exception.CandidacyAlreadyExistsException;
import com.lyubo_learning.cams.exception.JobListingNotOpenException;
import com.lyubo_learning.cams.exception.ResourceNotFoundException;
import com.lyubo_learning.cams.mapper.CandidacyMapper;
import com.lyubo_learning.cams.repository.CandidacyRepository;
import com.lyubo_learning.cams.repository.JobListingRepository;
import com.lyubo_learning.cams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidacyService {

    private final CandidacyRepository candidacyRepository;
    private final JobListingRepository jobListingRepository;
    private final JobListingService jobListingService;
    private final JobApplicationService jobApplicationService;
    private final UserRepository userRepository;
    private final CandidacyMapper mapper;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public CandidacyResponse apply(Long jobListingId) {
        User candidate = getAuthenticatedUser();

        // A plain findById, not the company-ownership check: the candidate does
        // not own this listing, they are applying to it.
        JobListing listing = jobListingRepository.findById(jobListingId)
                .orElseThrow(() -> new ResourceNotFoundException("Job listing not found"));

        // Phase 14's own check, independent of browse. Browse only ever surfaces
        // OPEN listings, but nothing stops a POST with an id obtained elsewhere —
        // a saved link, a shared id. It gates new applications only: archiving a
        // listing later leaves existing candidacies untouched.
        if (listing.getStatus() != JobListingStatus.OPEN) {
            throw new JobListingNotOpenException("This listing is no longer open for applications");
        }

        if (candidacyRepository.existsByCandidateIdAndJobListingId(candidate.getId(), listing.getId())) {
            throw new CandidacyAlreadyExistsException("You have already applied to this listing");
        }

        Candidacy candidacy = candidacyRepository.save(Candidacy.builder()
                .candidate(candidate)
                .jobListing(listing)
                .status(CandidacyStatus.SUBMITTED)
                .appliedAt(LocalDateTime.now())
                .build());

        // Same transaction: the candidate's personal tracker entry and the real
        // application to the employer are created together or not at all.
        jobApplicationService.createFromCandidacy(candidacy);

        return mapper.toResponse(candidacy);
    }

    @Transactional(readOnly = true)
    public List<CandidacyResponse> getMyCandidacies() {
        User candidate = getAuthenticatedUser();

        return candidacyRepository.findByCandidateIdFetchingListing(candidate.getId())
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CandidacyResponse> getCandidaciesForListing(Long jobListingId) {
        // Resolves and authorizes in one step, on the employer's behalf: throws
        // ResourceNotFoundException (404) for an unknown id and
        // UnauthorizedAccessException (403) for another company's listing.
        JobListing listing = jobListingService.getListingOwnedByCompany(jobListingId);

        return candidacyRepository.findByJobListingIdFetchingListing(listing.getId())
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

}
