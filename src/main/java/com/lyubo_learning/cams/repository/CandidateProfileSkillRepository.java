package com.lyubo_learning.cams.repository;

import com.lyubo_learning.cams.entity.CandidateProfileSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CandidateProfileSkillRepository extends JpaRepository<CandidateProfileSkill, Long> {

    List<CandidateProfileSkill> findByCandidateProfileId(Long candidateProfileId);

    void deleteByCandidateProfileId(Long candidateProfileId);

    // Loads a whole applicant list's skills in one query. The JOIN FETCH is
    // load-bearing: without it each skill stays a lazy proxy and the mapper
    // initialises them one at a time, same trap as the job-listing browse.
    @Query("""
            SELECT cps FROM CandidateProfileSkill cps
            JOIN FETCH cps.skill
            WHERE cps.candidateProfile.id IN :profileIds
            """)
    List<CandidateProfileSkill> findByCandidateProfileIdInFetchingSkill(
            @Param("profileIds") Collection<Long> profileIds);

}
