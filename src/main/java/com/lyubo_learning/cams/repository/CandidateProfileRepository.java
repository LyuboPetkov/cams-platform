package com.lyubo_learning.cams.repository;

import com.lyubo_learning.cams.entity.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {

    Optional<CandidateProfile> findByUserId(Long userId);

    // Batch form, for loading a whole applicant list's profiles in one query.
    // Applicants without a profile simply have no row here.
    List<CandidateProfile> findByUserIdIn(Collection<Long> userIds);

}
