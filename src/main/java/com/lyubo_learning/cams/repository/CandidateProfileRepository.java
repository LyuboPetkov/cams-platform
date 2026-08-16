package com.lyubo_learning.cams.repository;

import com.lyubo_learning.cams.entity.CandidateProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {

    Optional<CandidateProfile> findByUserId(Long userId);

    // Batch form, for loading a whole applicant list's profiles in one query.
    // Applicants without a profile simply have no row here.
    List<CandidateProfile> findByUserIdIn(Collection<Long> userIds);

    // The employer-facing match query, the mirror image of
    // JobListingRepository.findTopMatchesForCandidate. Excludes candidates who
    // already have a Candidacy against this listing — an employer already sees
    // those via GET /api/job-listings/{id}/candidacies; matches exists to
    // surface people who haven't applied, not repeat the applicant list.
    // c.candidate = p.user, not p.id: Candidacy.candidate and
    // CandidateProfile.user both key off the same User row, there's no
    // CandidateProfile FK on Candidacy to join through directly.
    @Query("""
            SELECT p FROM CandidateProfile p
            WHERE p.embedding IS NOT NULL
              AND NOT EXISTS (
                    SELECT 1 FROM Candidacy c
                    WHERE c.candidate = p.user AND c.jobListing.id = :jobListingId)
            ORDER BY cosine_distance(p.embedding, :embedding) ASC
            """)
    List<CandidateProfile> findTopMatchesForListing(@Param("embedding") float[] embedding,
                                                     @Param("jobListingId") Long jobListingId,
                                                     Pageable pageable);

}
