package com.lyubo_learning.cams.repository;

import com.lyubo_learning.cams.entity.CandidateExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateExperienceRepository extends JpaRepository<CandidateExperience, Long> {

    List<CandidateExperience> findByCandidateProfileId(Long candidateProfileId);

    void deleteByCandidateProfileId(Long candidateProfileId);

}
