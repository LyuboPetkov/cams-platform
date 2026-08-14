package com.lyubo_learning.cams.repository;

import com.lyubo_learning.cams.entity.CandidateProfileSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateProfileSkillRepository extends JpaRepository<CandidateProfileSkill, Long> {

    List<CandidateProfileSkill> findByCandidateProfileId(Long candidateProfileId);

    void deleteByCandidateProfileId(Long candidateProfileId);

}
