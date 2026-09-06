package cams.service;

import cams.client.EmbeddingClient;
import cams.entity.CandidateCourse;
import cams.entity.CandidateEducation;
import cams.entity.CandidateExperience;
import cams.entity.CandidateProfileSkill;
import cams.entity.JobListingSkill;
import cams.entity.Skill;
import cams.event.CandidateProfileChangedEvent;
import cams.event.JobListingChangedEvent;
import cams.repository.CandidateCourseRepository;
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
import java.util.List;

// Recomputes an embedding after the triggering transaction commits - never
// synchronously inside it.
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final CandidateProfileRepository candidateProfileRepository;
    private final CandidateProfileSkillRepository candidateProfileSkillRepository;
    private final CandidateExperienceRepository candidateExperienceRepository;
    private final CandidateEducationRepository candidateEducationRepository;
    private final CandidateCourseRepository candidateCourseRepository;
    private final JobListingRepository jobListingRepository;
    private final JobListingSkillRepository jobListingSkillRepository;
    private final EmbeddingClient embeddingClient;

    // extraParts is candidate-only (experience/education text); job listings
    // pass an empty list, keeping one shared assembly point for both types.
    private String buildEmbeddingText(String headlineOrTitle, String description, List<String> skillNames,
                                       List<String> extraParts) {
        List<String> parts = new ArrayList<>();
        if (headlineOrTitle != null && !headlineOrTitle.isBlank()) parts.add(headlineOrTitle);
        if (description != null && !description.isBlank()) parts.add(description);
        if (!skillNames.isEmpty()) parts.add("Skills: " + String.join(", ", skillNames));
        parts.addAll(extraParts);
        return String.join(". ", parts);
    }

    // companyName is deliberately left out: no listing asks for a specific employer,
    // and changing this text would invalidate the M0-M6 benchmark measured against it.
    private String describeExperience(CandidateExperience experience) {
        if (experience.getDescription() != null && !experience.getDescription().isBlank()) {
            return experience.getRoleTitle() + ": " + experience.getDescription();
        }
        return experience.getRoleTitle();
    }

    private String describeEducation(CandidateEducation education) {
        String base = education.getLevel() + " at " + education.getInstitutionName();
        if (education.getSpecialty() != null && !education.getSpecialty().isBlank()) {
            return base + " in " + education.getSpecialty();
        }
        return base;
    }

    private String describeCourse(CandidateCourse course) {
        return course.getName() + " (" + course.getIssuingOrganization() + ")";
    }

    // REQUIRES_NEW gives this async thread its own session, or the lazy Skill
    // proxies below throw LazyInitializationException. Propagation must be set
    // explicitly here - plain @Transactional on a listener is rejected at startup.
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCandidateProfileChanged(CandidateProfileChangedEvent event) {
        candidateProfileRepository.findById(event.candidateProfileId()).ifPresent(profile -> {
            // Name-ordered so the same skill set always yields the same vector on re-embed.
            List<String> skillNames = candidateProfileSkillRepository
                    .findByCandidateProfileIdOrderBySkillNameAsc(profile.getId())
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

            List<String> courseParts = candidateCourseRepository.findByCandidateProfileId(profile.getId())
                    .stream()
                    .map(this::describeCourse)
                    .toList();

            List<String> extraParts = new ArrayList<>();
            extraParts.addAll(experienceParts);
            extraParts.addAll(educationParts);
            // Course text is folded in experimentally; drop this line to revert.
            extraParts.addAll(courseParts);

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
            // Name-ordered so the same skill set always yields the same vector on re-embed.
            List<String> skillNames = jobListingSkillRepository
                    .findByJobListingIdOrderBySkillNameAsc(listing.getId())
                    .stream()
                    .map(JobListingSkill::getSkill)
                    .map(Skill::getName)
                    .toList();

            // Folds the listing's level into the embedded text, e.g.
            // "title. description. Skills: a, b. Level: MID". Null guard is
            // defensive only - level is NOT NULL.
            List<String> extraParts = new ArrayList<>();
            if (listing.getLevel() != null) {
                extraParts.add("Level: " + listing.getLevel());
            }

            String text = buildEmbeddingText(listing.getTitle(), listing.getDescription(), skillNames, extraParts);
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
