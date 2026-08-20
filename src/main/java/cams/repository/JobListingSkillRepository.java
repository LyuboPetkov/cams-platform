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

    // Loads a whole page's join rows in one query. The JOIN FETCH is load-bearing:
    // without it, jls.skill stays a lazy proxy and the mapper initialises each one
    // individually, trading an N+1 for a different N+1.
    @Query("SELECT jls FROM JobListingSkill jls JOIN FETCH jls.skill WHERE jls.jobListing.id IN :ids")
    List<JobListingSkill> findByJobListingIdInFetchingSkill(@Param("ids") List<Long> ids);

}
