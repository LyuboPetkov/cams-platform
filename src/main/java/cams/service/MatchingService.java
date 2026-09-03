package cams.service;

import cams.dto.JobListingMatchResponse;
import cams.dto.MatchScoreResponse;
import cams.entity.CandidateProfile;
import cams.entity.JobListing;
import cams.entity.JobListingSkill;
import cams.entity.JobListingStatus;
import cams.entity.Role;
import cams.entity.Skill;
import cams.entity.User;
import cams.exception.NoEmbeddingAvailableException;
import cams.exception.ResourceNotFoundException;
import cams.mapper.JobListingMapper;
import cams.repository.CandidateProfileRepository;
import cams.repository.JobListingRepository;
import cams.repository.JobListingSkillRepository;
import cams.repository.UserRepository;
import cams.util.EmbeddingSimilarity;
import cams.util.LocationEligibility;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchingService {

    // Mirrors JobListingService.MAX_PAGE_SIZE's role: a hard, undocumented-as-a-param
    // ceiling, not user-tunable.
    private static final int MAX_MATCHES = 10;

    private final JobListingRepository jobListingRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final JobListingSkillRepository jobListingSkillRepository;
    private final UserRepository userRepository;
    private final JobListingMapper jobListingMapper;
    private final EmbeddingSimilarity embeddingSimilarity;
    private final LocationEligibility locationEligibility;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public List<JobListingMatchResponse> getMatchesForCandidate() {
        User candidate = getAuthenticatedUser();

        CandidateProfile profile = candidateProfileRepository.findByUserId(candidate.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        if (profile.getEmbedding() == null) {
            throw new NoEmbeddingAvailableException(
                    "Your profile has no content to match against yet - add a headline, description or skills first");
        }

        List<JobListing> listings = jobListingRepository.findTopMatchesForCandidate(
                JobListingStatus.OPEN, profile.getEmbedding(), candidate.getId(),
                locationEligibility.extractCity(profile.getLocation()), PageRequest.of(0, MAX_MATCHES));

        Map<Long, List<Skill>> skillsByListing = getSkillsOfAllListings(listings);

        return listings.stream()
                .map(listing -> JobListingMatchResponse.builder()
                        .listing(jobListingMapper.toBrowseResponse(
                                listing, skillsByListing.getOrDefault(listing.getId(), List.of())))
                        .similarityScore(embeddingSimilarity.cosineSimilarity(profile.getEmbedding(), listing.getEmbedding()))
                        .build())
                .toList();
    }

    // Single-listing score for the listing detail page, computed on demand when a
    // candidate opens a listing from browse. Unlike getMatchesForCandidate() a
    // missing embedding on either side is not an error here - it degrades to a
    // null score and the page simply omits the badge. Not gated on having a
    // Candidacy; non-candidate callers always get null.
    @Transactional(readOnly = true)
    public MatchScoreResponse getMatchScoreForListing(Long listingId) {
        User user = getAuthenticatedUser();

        JobListing listing = jobListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Job listing not found"));

        if (user.getRole() != Role.CANDIDATE) {
            return new MatchScoreResponse(null);
        }

        CandidateProfile profile = candidateProfileRepository.findByUserId(user.getId()).orElse(null);

        if (listing.getEmbedding() == null || profile == null || profile.getEmbedding() == null) {
            return new MatchScoreResponse(null);
        }

        return new MatchScoreResponse(
                embeddingSimilarity.cosineSimilarity(profile.getEmbedding(), listing.getEmbedding()));
    }

    private Map<Long, List<Skill>> getSkillsOfAllListings(List<JobListing> listings) {
        if (listings.isEmpty()) {
            return Map.of();
        }

        List<Long> ids = listings.stream().map(JobListing::getId).toList();

        return jobListingSkillRepository.findByJobListingIdInFetchingSkill(ids)
                .stream()
                .collect(Collectors.groupingBy(
                        link -> link.getJobListing().getId(),
                        Collectors.mapping(JobListingSkill::getSkill, Collectors.toList())));
    }

}
