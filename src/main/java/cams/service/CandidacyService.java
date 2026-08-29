package cams.service;

import cams.dto.CandidacyApplicantResponse;
import cams.dto.CandidacyResponse;
import cams.dto.CandidateProfileResponse;
import cams.entity.ApplicationStatus;
import cams.entity.Candidacy;
import cams.entity.CandidacyStatus;
import cams.entity.CandidateProfile;
import cams.entity.CandidateProfileSkill;
import cams.entity.JobListing;
import cams.entity.JobListingStatus;
import cams.entity.Skill;
import cams.entity.User;
import cams.exception.CandidacyAlreadyDecidedException;
import cams.exception.CandidacyAlreadyExistsException;
import cams.exception.JobListingNotOpenException;
import cams.exception.ResourceNotFoundException;
import cams.mapper.CandidacyMapper;
import cams.repository.CandidacyRepository;
import cams.repository.CandidateProfileRepository;
import cams.repository.CandidateProfileSkillRepository;
import cams.repository.JobListingRepository;
import cams.repository.UserRepository;
import cams.util.EmbeddingSimilarity;
import cams.util.LocationEligibility;
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
    private final CandidateProfileService candidateProfileService;
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CandidateProfileSkillRepository candidateProfileSkillRepository;
    private final CandidacyMapper mapper;
    private final EmbeddingSimilarity embeddingSimilarity;
    private final LocationEligibility locationEligibility;

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

        // Re-checked here because a POST can carry an id obtained outside browse.
        // Gates new applications only - archiving later leaves candidacies untouched.
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
        // Resolves and authorizes in one step: 404 for an unknown id, 403 for
        // another company's listing.
        JobListing listing = jobListingService.getListingOwnedByCompany(jobListingId);

        List<Candidacy> candidacies =
                candidacyRepository.findByJobListingIdFetchingListingAndCandidate(listing.getId());

        if (candidacies.isEmpty()) {
            return List.of();
        }

        // Two batch lookups for the whole page rather than two per applicant.
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
                    // Both may legitimately be absent - applying needs neither a
                    // filled-in profile nor any skills.
                    CandidateProfile profile = profilesByUserId.get(candidacy.getCandidate().getId());
                    List<Skill> skills = profile == null
                            ? List.of()
                            : skillsByProfileId.getOrDefault(profile.getId(), List.of());

                    Double matchScore = computeMatchScore(listing, profile);
                    boolean locationMismatch = !locationEligibility.isEligible(
                            profile == null ? null : profile.getLocation(), listing.getLocation(), listing.getRemote());
                    boolean openToRemoteMismatch = Boolean.TRUE.equals(listing.getRemote())
                            && profile != null && Boolean.FALSE.equals(profile.getOpenToRemote());

                    return mapper.toApplicantResponse(
                            candidacy, profile, skills, matchScore, locationMismatch, openToRemoteMismatch);
                })
                .toList();
    }

    // A missing embedding on either side degrades to a null score, never an
    // error or a dropped applicant row.
    private Double computeMatchScore(JobListing listing, CandidateProfile profile) {
        if (listing.getEmbedding() == null || profile == null || profile.getEmbedding() == null) {
            return null;
        }

        return embeddingSimilarity.cosineSimilarity(listing.getEmbedding(), profile.getEmbedding());
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

    // Same ownership check as accept()/reject(): any employer at the owning
    // company may view the full profile, not just whoever posted the listing.
    @Transactional(readOnly = true)
    public CandidateProfileResponse getCandidateProfileForEmployer(Long candidacyId) {
        Candidacy candidacy = candidacyRepository.findById(candidacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidacy not found"));

        jobListingService.getListingOwnedByCompany(candidacy.getJobListing().getId());

        return candidateProfileService.getProfileForUser(candidacy.getCandidate().getId());
    }

    @Transactional
    public CandidacyResponse accept(Long candidacyId) {
        return transitionStatus(candidacyId, CandidacyStatus.ACCEPTED, ApplicationStatus.INTERVIEWING);
    }

    @Transactional
    public CandidacyResponse reject(Long candidacyId) {
        return transitionStatus(candidacyId, CandidacyStatus.REJECTED, ApplicationStatus.REJECTED);
    }

    @Transactional
    public CandidacyResponse offer(Long candidacyId) {
        return transitionStatus(candidacyId, CandidacyStatus.OFFERED, ApplicationStatus.OFFERED);
    }

    // One body for all decisions - ownership check, allowed-transition guard and
    // linked-application sync are identical. The target state comes from the
    // endpoint, never a request body.
    private CandidacyResponse transitionStatus(Long candidacyId,
                                               CandidacyStatus target,
                                               ApplicationStatus linkedStatus) {
        Candidacy candidacy = candidacyRepository.findById(candidacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidacy not found"));

        // Same company-scoped authorization as the read.
        jobListingService.getListingOwnedByCompany(candidacy.getJobListing().getId());

        if (!isTransitionAllowed(candidacy.getStatus(), target)) {
            throw new CandidacyAlreadyDecidedException("This candidacy has already been decided");
        }

        candidacy.setStatus(target);
        Candidacy saved = candidacyRepository.save(candidacy);

        // Same transaction: the candidacy decision and the candidate's tracker
        // entry move together or not at all.
        jobApplicationService.syncStatusFromCandidacy(saved, linkedStatus);

        return mapper.toResponse(saved);
    }

    // SUBMITTED is the only entry point for ACCEPTED. OFFERED is the post-interview
    // step, reachable only from ACCEPTED. REJECTED is reachable both straight from
    // SUBMITTED and as a post-interview outcome from ACCEPTED. OFFERED and any
    // REJECTED stay terminal - no further move out of them.
    private boolean isTransitionAllowed(CandidacyStatus current, CandidacyStatus target) {
        return switch (target) {
            case ACCEPTED -> current == CandidacyStatus.SUBMITTED;
            case OFFERED -> current == CandidacyStatus.ACCEPTED;
            case REJECTED -> current == CandidacyStatus.SUBMITTED || current == CandidacyStatus.ACCEPTED;
            default -> false;
        };
    }

}
