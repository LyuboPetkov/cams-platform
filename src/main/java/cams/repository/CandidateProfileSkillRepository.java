package cams.repository;

import cams.entity.CandidateProfileSkill;
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

    // MUST use this, not findByCandidateProfileId: the name ordering makes
    // re-embedding the same skill set produce the same text, and so the same vector.
    @Query("""
            SELECT cps FROM CandidateProfileSkill cps
            JOIN FETCH cps.skill s
            WHERE cps.candidateProfile.id = :candidateProfileId
            ORDER BY s.name ASC
            """)
    List<CandidateProfileSkill> findByCandidateProfileIdOrderBySkillNameAsc(
            @Param("candidateProfileId") Long candidateProfileId);

    // Loads a whole applicant list's skills in one query. JOIN FETCH is
    // load-bearing - without it each skill stays a lazy proxy (N+1).
    @Query("""
            SELECT cps FROM CandidateProfileSkill cps
            JOIN FETCH cps.skill
            WHERE cps.candidateProfile.id IN :profileIds
            """)
    List<CandidateProfileSkill> findByCandidateProfileIdInFetchingSkill(
            @Param("profileIds") Collection<Long> profileIds);

}
