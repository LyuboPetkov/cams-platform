package cams.service;

import cams.client.EmbeddingClient;
import cams.entity.CandidateEducation;
import cams.entity.CandidateExperience;
import cams.entity.CandidateProfileSkill;
import cams.entity.JobListingSkill;
import cams.entity.Skill;
import cams.event.CandidateProfileChangedEvent;
import cams.event.JobListingChangedEvent;
import cams.repository.CandidateEducationRepository;
import cams.repository.CandidateExperienceRepository;
import cams.repository.CandidateProfileRepository;
import cams.repository.CandidateProfileSkillRepository;
import cams.repository.JobListingRepository;
import cams.repository.JobListingSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Listens for the four save-time trigger points (Section 3.5 of the Phase 16
// brief) and (re)computes an embedding after the triggering transaction has
// actually committed — never synchronously inside it.
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final CandidateProfileRepository candidateProfileRepository;
    private final CandidateProfileSkillRepository candidateProfileSkillRepository;
    private final CandidateExperienceRepository candidateExperienceRepository;
    private final CandidateEducationRepository candidateEducationRepository;
    private final JobListingRepository jobListingRepository;
    private final JobListingSkillRepository jobListingSkillRepository;
    private final EmbeddingClient embeddingClient;

    // extraParts is candidate-only (experience/education text, Phase 21b Section
    // 3.6) — job listings pass an empty list, so this stays the one shared
    // assembly point for both entity types rather than forking into two methods.
    private String buildEmbeddingText(String headlineOrTitle, String description, List<String> skillNames,
                                       List<String> extraParts) {
        List<String> parts = new ArrayList<>();
        if (headlineOrTitle != null && !headlineOrTitle.isBlank()) parts.add(headlineOrTitle);
        if (description != null && !description.isBlank()) parts.add(description);
        if (!skillNames.isEmpty()) parts.add("Skills: " + String.join(", ", skillNames));
        parts.addAll(extraParts);
        return String.join(". ", parts);
    }

    private String describeExperience(CandidateExperience experience) {
        if (experience.getDescription() != null && !experience.getDescription().isBlank()) {
            return experience.getRoleTitle() + ": " + experience.getDescription();
        }
        return experience.getRoleTitle();
    }

    private String describeEducation(CandidateEducation education) {
        return education.getLevel() + " at " + education.getInstitutionName();
    }

    // @Transactional opens the async thread's own session — the thread has none
    // otherwise, since transactions are thread-bound and the outer transaction
    // that published this event already committed and closed on a different
    // thread. Without it, the lazy Skill proxies below throw
    // LazyInitializationException: "no session". Spring requires
    // REQUIRES_NEW/NOT_SUPPORTED explicitly on a @TransactionalEventListener —
    // plain @Transactional is rejected at startup.
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCandidateProfileChanged(CandidateProfileChangedEvent event) {
        candidateProfileRepository.findById(event.candidateProfileId()).ifPresent(profile -> {
            List<String> skillNames = candidateProfileSkillRepository.findByCandidateProfileId(profile.getId())
                    .stream()
                    .map(CandidateProfileSkill::getSkill)
                    .map(Skill::getName)
                    .toList();

            List<String> experienceParts = candidateExperienceRepository.findByCandidateProfileId(profile.getId())
                    .stream()
                    .map(this::describeExperience)
                    .toList();

            List<String> educationParts = candidateEducationRepository.findByCandidateProfileId(profile.getId())
                    .stream()
                    .map(this::describeEducation)
                    .toList();

            List<String> extraParts = new ArrayList<>();
            extraParts.addAll(experienceParts);
            extraParts.addAll(educationParts);

            String text = buildEmbeddingText(profile.getHeadline(), profile.getDescription(), skillNames, extraParts);
            if (text.isBlank()) return;

            try {
                float[] embedding = embeddingClient.embed(text);
                profile.setEmbedding(embedding);
                profile.setEmbeddingUpdatedAt(LocalDateTime.now());
                candidateProfileRepository.save(profile);
            } catch (Exception e) {
                log.warn("Embedding generation failed for candidate profile {}", event.candidateProfileId(), e);
            }
        });
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobListingChanged(JobListingChangedEvent event) {
        jobListingRepository.findById(event.jobListingId()).ifPresent(listing -> {
            List<String> skillNames = jobListingSkillRepository.findByJobListingId(listing.getId())
                    .stream()
                    .map(JobListingSkill::getSkill)
                    .map(Skill::getName)
                    .toList();
            String text = buildEmbeddingText(listing.getTitle(), listing.getDescription(), skillNames,
                    Collections.emptyList());
            if (text.isBlank()) return;

            try {
                float[] embedding = embeddingClient.embed(text);
                listing.setEmbedding(embedding);
                listing.setEmbeddingUpdatedAt(LocalDateTime.now());
                jobListingRepository.save(listing);
            } catch (Exception e) {
                log.warn("Embedding generation failed for job listing {}", event.jobListingId(), e);
            }
        });
    }
}
