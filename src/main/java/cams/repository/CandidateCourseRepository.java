package cams.repository;

import cams.entity.CandidateCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateCourseRepository extends JpaRepository<CandidateCourse, Long> {

    List<CandidateCourse> findByCandidateProfileId(Long candidateProfileId);

    void deleteByCandidateProfileId(Long candidateProfileId);

}
