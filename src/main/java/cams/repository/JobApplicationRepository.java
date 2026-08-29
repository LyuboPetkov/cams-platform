package cams.repository;

import cams.entity.ApplicationSource;
import cams.entity.ApplicationStatus;
import cams.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    List<JobApplication> findByUserId(Long userId);

    // JobApplication.candidacy is the owning side and Candidacy has no
    // back-reference, so reaching the linked row from a candidacy needs this.
    Optional<JobApplication> findByCandidacyId(Long candidacyId);

    boolean existsByIdAndUserId(Long id, Long userId);

    List<JobApplication> findByUserIdAndStatus(Long userId, ApplicationStatus status);

    List<JobApplication> findByUserIdAndSource(Long userId, ApplicationSource source);

    List<JobApplication> findByUserIdAndStatusAndSource(Long userId, ApplicationStatus status, ApplicationSource source);

}
