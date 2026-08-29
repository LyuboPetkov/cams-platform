package cams.repository;

import cams.entity.JobLevel;
import cams.entity.JobListing;
import cams.entity.JobListingStatus;
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

    // Candidate-facing browse: forces status OPEN so no caller can request
    // archived listings. Passes level by name for PostgreSQL enum reasons (see browseByStatus).
    default Page<JobListing> browse(String location,
                                    Boolean remote,
                                    JobLevel level,
                                    Long companyId,
                                    boolean skillFilterOff,
                                    List<Long> skillIds,
                                    Pageable pageable) {
        return browseByStatus(JobListingStatus.OPEN,
                location,
                remote,
                level == null ? null : level.name(),
                companyId,
                skillFilterOff,
                skillIds,
                pageable);
    }

    // Every filter is optional: a null parameter drops out of the WHERE clause.
    // status and level are bound by name, not as enum literals; the CAST(... AS STRING)
    // wrappers are load-bearing - PostgreSQL can't infer a type for a null bind otherwise.
    @Query("""
            SELECT l FROM JobListing l
            WHERE l.status = :status
              AND (CAST(:location AS STRING) IS NULL
                   OR LOWER(l.location) LIKE LOWER(CONCAT('%', CAST(:location AS STRING), '%')))
              AND (:remote IS NULL OR l.remote = :remote)
              AND (CAST(:level AS STRING) IS NULL OR CAST(l.level AS STRING) = CAST(:level AS STRING))
              AND (:companyId IS NULL OR l.company.id = :companyId)
              AND (:skillFilterOff = TRUE OR EXISTS (
                    SELECT 1 FROM JobListingSkill jls
                    WHERE jls.jobListing = l AND jls.skill.id IN :skillIds))
            """)
    Page<JobListing> browseByStatus(@Param("status") JobListingStatus status,
                                    @Param("location") String location,
                                    @Param("remote") Boolean remote,
                                    @Param("level") String level,
                                    @Param("companyId") Long companyId,
                                    @Param("skillFilterOff") boolean skillFilterOff,
                                    @Param("skillIds") List<Long> skillIds,
                                    Pageable pageable);

    // Candidate-facing match query, ordered by embedding cosine distance. Skips
    // un-embedded listings and any the candidate already has a Candidacy against.
    // The location clause is a substring heuristic (see LocationEligibility); null on either side means "no constraint".
    @Query("""
            SELECT l FROM JobListing l
            WHERE l.status = :status
              AND l.embedding IS NOT NULL
              AND NOT EXISTS (
                    SELECT 1 FROM Candidacy c
                    WHERE c.jobListing = l AND c.candidate.id = :candidateId)
              AND (l.remote = TRUE
                   OR CAST(:candidateCity AS STRING) IS NULL
                   OR l.location IS NULL
                   OR LOWER(l.location) LIKE CONCAT('%', LOWER(CAST(:candidateCity AS STRING)), '%'))
            ORDER BY cosine_distance(l.embedding, :embedding) ASC
            """)
    List<JobListing> findTopMatchesForCandidate(@Param("status") JobListingStatus status,
                                                @Param("embedding") float[] embedding,
                                                @Param("candidateId") Long candidateId,
                                                @Param("candidateCity") String candidateCity,
                                                Pageable pageable);

}
