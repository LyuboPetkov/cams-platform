package cams.repository;

import cams.entity.JobListingSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobListingSkillRepository extends JpaRepository<JobListingSkill, Long> {

    List<JobListingSkill> findByJobListingId(Long jobListingId);

    // MUST use this, not findByJobListingId: the name ordering makes re-embedding
    // the same skill set produce the same text, and so the same vector.
    @Query("""
            SELECT jls FROM JobListingSkill jls
            JOIN FETCH jls.skill s
            WHERE jls.jobListing.id = :jobListingId
            ORDER BY s.name ASC
            """)
    List<JobListingSkill> findByJobListingIdOrderBySkillNameAsc(@Param("jobListingId") Long jobListingId);

    // Loads a whole page's join rows in one query. JOIN FETCH is load-bearing -
    // without it jls.skill stays a lazy proxy (N+1).
    @Query("SELECT jls FROM JobListingSkill jls JOIN FETCH jls.skill WHERE jls.jobListing.id IN :ids")
    List<JobListingSkill> findByJobListingIdInFetchingSkill(@Param("ids") List<Long> ids);

}
