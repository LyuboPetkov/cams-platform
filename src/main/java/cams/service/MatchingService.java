package cams.service;

import cams.dto.CandidateMatchResponse;
import cams.dto.JobListingMatchResponse;
import cams.entity.CandidateProfile;
import cams.entity.CandidateProfileSkill;
import cams.entity.JobListing;
import cams.entity.JobListingSkill;
import cams.entity.JobListingStatus;
import cams.entity.Skill;
import cams.entity.User;
import cams.exception.NoEmbeddingAvailableException;
import cams.exception.ResourceNotFoundException;
import cams.mapper.CandidacyMapper;
import cams.mapper.JobListingMapper;
import cams.repository.CandidateProfileRepository;
import cams.repository.CandidateProfileSkillRepository;
import cams.repository.JobListingRepository;
import cams.repository.JobListingSkillRepository;
import cams.repository.UserRepository;
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
    // ceiling, not user-tunable this phase.
    private static final int MAX_MATCHES = 10;

    private final JobListingRepository jobListingRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final JobListingSkillRepository jobListingSkillRepository;
    private final CandidateProfileSkillRepository candidateProfileSkillRepository;
    private final UserRepository userRepository;
    private final JobListingService jobListingService;
    private final JobListingMapper jobListingMapper;
    private final CandidacyMapper candidacyMapper;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    @Transactional(readOnly = true)
    public List<JobListingMatchResponse> getMatchesForCandidate() {
        User candidate = getAuthenticatedUser();

        CandidateProfile profile = candidateProfileRepository.findByUserId(candidate.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        if (profile.getEmbedding() == null) {
            throw new NoEmbeddingAvailableException(
                    "Your profile has no content to match against yet — add a headline, description or skills first");
        }

        List<JobListing> listings = jobListingRepository.findTopMatchesForCandidate(
                JobListingStatus.OPEN, profile.getEmbedding(), candidate.getId(),
                PageRequest.of(0, MAX_MATCHES));

        Map<Long, List<Skill>> skillsByListing = getSkillsOfAllListings(listings);

        return listings.stream()
                .map(listing -> JobListingMatchResponse.builder()
                        .listing(jobListingMapper.toBrowseResponse(
                                listing, skillsByListing.getOrDefault(listing.getId(), List.of())))
                        .similarityScore(cosineSimilarity(profile.getEmbedding(), listing.getEmbedding()))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CandidateMatchResponse> getMatchesForListing(Long jobListingId) {
        JobListing listing = jobListingService.getListingOwnedByCompany(jobListingId);

        if (listing.getEmbedding() == null) {
            throw new NoEmbeddingAvailableException(
                    "This listing has no embedding yet — it may have just been created; try again shortly");
        }

        List<CandidateProfile> profiles = candidateProfileRepository.findTopMatchesForListing(
                listing.getEmbedding(), jobListingId, PageRequest.of(0, MAX_MATCHES));

        Map<Long, List<Skill>> skillsByProfile = getSkillsOfAllProfiles(profiles);

        List<Long> userIds = profiles.stream().map(profile -> profile.getUser().getId()).toList();
        Map<Long, User> usersById = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        return profiles.stream()
                .map(profile -> {
                    User user = usersById.get(profile.getUser().getId());
                    List<Skill> skills = skillsByProfile.getOrDefault(profile.getId(), List.of());

                    return CandidateMatchResponse.builder()
                            .candidate(candidacyMapper.toCandidateSummary(user, profile, skills))
                            .similarityScore(cosineSimilarity(listing.getEmbedding(), profile.getEmbedding()))
                            .build();
                })
                .toList();
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

    private Map<Long, List<Skill>> getSkillsOfAllProfiles(List<CandidateProfile> profiles) {
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

}
