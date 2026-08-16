package com.lyubo_learning.cams.repository;

import com.lyubo_learning.cams.entity.JobLevel;
import com.lyubo_learning.cams.entity.JobListing;
import com.lyubo_learning.cams.entity.JobListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobListingRepository extends JpaRepository<JobListing, Long> {

    List<JobListing> findByCompanyId(Long companyId);

    // The candidate-facing browse query. This wrapper is what the service calls;
    // it fixes status to OPEN here, in the repository, so no caller can ask for
    // archived listings, and it converts the level to its name for the query
    // below. Both conversions exist for PostgreSQL type reasons explained there.
    default Page<JobListing> browse(String location,
                                    Boolean remote,
                                    JobLevel level,
                                    boolean skillFilterOff,
                                    List<Long> skillIds,
                                    Pageable pageable) {
        return browseByStatus(JobListingStatus.OPEN,
                location,
                remote,
                level == null ? null : level.name(),
                skillFilterOff,
                skillIds,
                pageable);
    }

    // Every filter is optional: a null parameter drops out of the WHERE clause.
    //
    // Two PostgreSQL-specific details here, both found by running this query
    // before anything was built on top of it:
    //
    // 1. `status` is a bound parameter, not the JPQL literal
    //    `l.status = JobListingStatus.OPEN`. Hibernate renders such a literal as
    //    'OPEN'::JobListingStatus — a cast to the *Java* class name — while the
    //    PostgreSQL type is job_listing_status, so the query dies with
    //    `type "joblistingstatus" does not exist`. Bound as a parameter it goes
    //    through @JdbcType(PostgreSQLEnumJdbcType.class) and binds correctly.
    //
    // 2. The CASTs are load-bearing, not decoration. A null parameter reaches
    //    PostgreSQL with no type attached, and it cannot infer one from
    //    `? IS NULL` alone: an unfiltered browse failed with
    //    `function lower(bytea) does not exist` (location) and
    //    `could not determine data type of parameter` (level). CAST tells the
    //    planner the type regardless of which branch of the OR is taken.
    //    `level` is bound as a String and compared against a cast of the column
    //    for the same reason — casting a null straight to the native enum type
    //    is what fails. `remote` needs no cast: PostgreSQL resolves a null
    //    boolean parameter on its own, verified in the same run.
    @Query("""
            SELECT l FROM JobListing l
            WHERE l.status = :status
              AND (CAST(:location AS STRING) IS NULL
                   OR LOWER(l.location) LIKE LOWER(CONCAT('%', CAST(:location AS STRING), '%')))
              AND (:remote IS NULL OR l.remote = :remote)
              AND (CAST(:level AS STRING) IS NULL OR CAST(l.level AS STRING) = CAST(:level AS STRING))
              AND (:skillFilterOff = TRUE OR EXISTS (
                    SELECT 1 FROM JobListingSkill jls
                    WHERE jls.jobListing = l AND jls.skill.id IN :skillIds))
            """)
    Page<JobListing> browseByStatus(@Param("status") JobListingStatus status,
                                    @Param("location") String location,
                                    @Param("remote") Boolean remote,
                                    @Param("level") String level,
                                    @Param("skillFilterOff") boolean skillFilterOff,
                                    @Param("skillIds") List<Long> skillIds,
                                    Pageable pageable);

}
