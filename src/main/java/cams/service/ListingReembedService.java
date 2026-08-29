package cams.service;

import cams.entity.JobListing;
import cams.event.JobListingChangedEvent;
import cams.repository.JobListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Backfills existing listings after a change to how the embedded text is built,
// republishing JobListingChangedEvent so the async pipeline re-embeds each one.
// Admin-triggered and one-off, not a CommandLineRunner that would re-run every deploy.
@Service
@RequiredArgsConstructor
public class ListingReembedService {

    private final JobListingRepository jobListingRepository;
    private final ApplicationEventPublisher eventPublisher;

    // @Transactional so EmbeddingService's AFTER_COMMIT listener fires at all -
    // events published with no surrounding transaction are silently dropped.
    // Returns as soon as the re-embed events are queued.
    @Transactional
    public int reembedAllListings() {
        List<Long> ids = jobListingRepository.findAll()
                .stream()
                .map(JobListing::getId)
                .toList();

        ids.forEach(id -> eventPublisher.publishEvent(new JobListingChangedEvent(id)));

        return ids.size();
    }
}
