package com.lyubo_learning.cams.repository;

import com.lyubo_learning.cams.entity.Candidacy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidacyRepository extends JpaRepository<Candidacy, Long> {

    // The friendly early exit in front of uq_candidacies_candidate_listing.
    boolean existsByCandidateIdAndJobListingId(Long candidateId, Long jobListingId);

    // Both list queries fetch-join the listing and its company because
    // CandidacyMapper reads jobListingTitle and companyName off that chain; a
    // plain derived query would initialise two lazy proxies per row.
    @Query("""
            SELECT c FROM Candidacy c
            JOIN FETCH c.jobListing l
            JOIN FETCH l.company
            WHERE c.candidate.id = :candidateId
            ORDER BY c.appliedAt DESC, c.id DESC
            """)
    List<Candidacy> findByCandidateIdFetchingListing(@Param("candidateId") Long candidateId);

    @Query("""
            SELECT c FROM Candidacy c
            JOIN FETCH c.jobListing l
            JOIN FETCH l.company
            WHERE l.id = :jobListingId
            ORDER BY c.appliedAt DESC, c.id DESC
            """)
    List<Candidacy> findByJobListingIdFetchingListing(@Param("jobListingId") Long jobListingId);

}
