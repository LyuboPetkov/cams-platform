package cams.service;

import cams.entity.CandidateProfile;
import cams.event.CandidateProfileChangedEvent;
import cams.repository.CandidateProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Republishes CandidateProfileChangedEvent for every profile so the existing
// async embedding pipeline recomputes and saves each one.
@Service
@RequiredArgsConstructor
public class CandidateReembedService {

    private final CandidateProfileRepository candidateProfileRepository;
    private final ApplicationEventPublisher eventPublisher;

    // @Transactional so EmbeddingService's AFTER_COMMIT listener actually fires
    // (silently dropped otherwise). Returns as soon as events are queued;
    // each recomputes its embedding on its own async thread.
    @Transactional
    public int reembedAllCandidateProfiles() {
        List<Long> ids = candidateProfileRepository.findAll()
                .stream()
                .map(CandidateProfile::getId)
                .toList();

        ids.forEach(id -> eventPublisher.publishEvent(new CandidateProfileChangedEvent(id)));

        return ids.size();
    }
}
