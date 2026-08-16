package com.lyubo_learning.cams.service;

import com.lyubo_learning.cams.dto.CandidacyApplicantResponse;
import com.lyubo_learning.cams.dto.CandidacyResponse;
import com.lyubo_learning.cams.entity.ApplicationStatus;
import com.lyubo_learning.cams.entity.Candidacy;
import com.lyubo_learning.cams.entity.CandidacyStatus;
import com.lyubo_learning.cams.entity.CandidateProfile;
import com.lyubo_learning.cams.entity.CandidateProfileSkill;
import com.lyubo_learning.cams.entity.JobListing;
import com.lyubo_learning.cams.entity.JobListingStatus;
import com.lyubo_learning.cams.entity.Skill;
import com.lyubo_learning.cams.entity.User;
import com.lyubo_learning.cams.exception.CandidacyAlreadyDecidedException;
import com.lyubo_learning.cams.exception.CandidacyAlreadyExistsException;
import com.lyubo_learning.cams.exception.JobListingNotOpenException;
import com.lyubo_learning.cams.exception.ResourceNotFoundException;
import com.lyubo_learning.cams.mapper.CandidacyMapper;
import com.lyubo_learning.cams.repository.CandidacyRepository;
import com.lyubo_learning.cams.repository.CandidateProfileRepository;
import com.lyubo_learning.cams.repository.CandidateProfileSkillRepository;
import com.lyubo_learning.cams.repository.JobListingRepository;
import com.lyubo_learning.cams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidacyService {

    private final CandidacyRepository candidacyRepository;
    private final JobListingRepository jobListingRepository;
    private final JobListingService jobListingService;
    private final JobApplicationService jobApplicationService;
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CandidateProfileSkillRepository candidateProfileSkillRepository;
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
    public List<CandidacyApplicantResponse> getCandidaciesForListing(Long jobListingId) {
        // Resolves and authorizes in one step, on the employer's behalf: throws
        // ResourceNotFoundException (404) for an unknown id and
        // UnauthorizedAccessException (403) for another company's listing.
        JobListing listing = jobListingService.getListingOwnedByCompany(jobListingId);

        List<Candidacy> candidacies =
                candidacyRepository.findByJobListingIdFetchingListingAndCandidate(listing.getId());

        if (candidacies.isEmpty()) {
            return List.of();
        }

        // Two batch lookups for the whole page rather than two per applicant,
        // the same shape browseListings() uses for listing skills.
        List<Long> candidateIds = candidacies.stream()
                .map(candidacy -> candidacy.getCandidate().getId())
                .distinct()
                .toList();

        Map<Long, CandidateProfile> profilesByUserId = candidateProfileRepository.findByUserIdIn(candidateIds)
                .stream()
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), profile -> profile));

        Map<Long, List<Skill>> skillsByProfileId = getSkillsOfAll(profilesByUserId.values());

        return candidacies.stream()
                .map(candidacy -> {
                    // Both may legitimately be absent: applying does not require a
                    // filled-in profile, and a profile need not have any skills.
                    CandidateProfile profile = profilesByUserId.get(candidacy.getCandidate().getId());
                    List<Skill> skills = profile == null
                            ? List.of()
                            : skillsByProfileId.getOrDefault(profile.getId(), List.of());

                    return mapper.toApplicantResponse(candidacy, profile, skills);
                })
                .toList();
    }

    private Map<Long, List<Skill>> getSkillsOfAll(Collection<CandidateProfile> profiles) {
        if (profiles.isEmpty()) {
            return Map.of();
        }

        List<Long> profileIds = profiles.stream().map(CandidateProfile::getId).toList();

        return candidateProfileSkillRepository.findByCandidateProfileIdInFetchingSkill(profileIds)
                .stream()
                .collect(Collectors.groupingBy(
                        link -> link.getCandidateProfile().getId(),
                        Collectors.mapping(CandidateProfileSkill::getSkill, Collectors.toList())));
    }

    @Transactional
    public CandidacyResponse accept(Long candidacyId) {
        return transitionStatus(candidacyId, CandidacyStatus.ACCEPTED, ApplicationStatus.INTERVIEWING);
    }

    @Transactional
    public CandidacyResponse reject(Long candidacyId) {
        return transitionStatus(candidacyId, CandidacyStatus.REJECTED, ApplicationStatus.REJECTED);
    }

    // One body for both decisions: the ownership check, the terminal-state guard
    // and the linked-application sync are identical either way. The target state
    // is never read from a request body — choosing the endpoint IS the choice,
    // the same way archiveListing() takes no status field.
    private CandidacyResponse transitionStatus(Long candidacyId,
                                               CandidacyStatus target,
                                               ApplicationStatus linkedStatus) {
        Candidacy candidacy = candidacyRepository.findById(candidacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidacy not found"));

        // Same company-scoped authorization as the read: any employer at the
        // owning company may decide, not only whoever posted the listing.
        jobListingService.getListingOwnedByCompany(candidacy.getJobListing().getId());

        // Terminal in both directions — no un-accept, no un-reject, no switching
        // one decision for the other.
        if (candidacy.getStatus() != CandidacyStatus.SUBMITTED) {
            throw new CandidacyAlreadyDecidedException("This candidacy has already been decided");
        }

        candidacy.setStatus(target);
        Candidacy saved = candidacyRepository.save(candidacy);

        // Inside this method's transaction: the candidacy decision and the
        // candidate's tracker entry move together or not at all. Leaving the
        // tracker on APPLIED after a decision would recreate exactly the
        // inconsistency Phase 14's edit restriction exists to prevent.
        jobApplicationService.syncStatusFromCandidacy(saved, linkedStatus);

        return mapper.toResponse(saved);
    }

}
