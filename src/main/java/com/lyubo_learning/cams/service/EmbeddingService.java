package com.lyubo_learning.cams.service;

import com.lyubo_learning.cams.client.EmbeddingClient;
import com.lyubo_learning.cams.entity.CandidateProfileSkill;
import com.lyubo_learning.cams.entity.JobListingSkill;
import com.lyubo_learning.cams.entity.Skill;
import com.lyubo_learning.cams.event.CandidateProfileChangedEvent;
import com.lyubo_learning.cams.event.JobListingChangedEvent;
import com.lyubo_learning.cams.repository.CandidateProfileRepository;
import com.lyubo_learning.cams.repository.CandidateProfileSkillRepository;
import com.lyubo_learning.cams.repository.JobListingRepository;
import com.lyubo_learning.cams.repository.JobListingSkillRepository;
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
    private final JobListingRepository jobListingRepository;
    private final JobListingSkillRepository jobListingSkillRepository;
    private final EmbeddingClient embeddingClient;

    private String buildEmbeddingText(String headlineOrTitle, String description, List<String> skillNames) {
        List<String> parts = new ArrayList<>();
        if (headlineOrTitle != null && !headlineOrTitle.isBlank()) parts.add(headlineOrTitle);
        if (description != null && !description.isBlank()) parts.add(description);
        if (!skillNames.isEmpty()) parts.add("Skills: " + String.join(", ", skillNames));
        return String.join(". ", parts);
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
            String text = buildEmbeddingText(profile.getHeadline(), profile.getDescription(), skillNames);
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
            String text = buildEmbeddingText(listing.getTitle(), listing.getDescription(), skillNames);
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
