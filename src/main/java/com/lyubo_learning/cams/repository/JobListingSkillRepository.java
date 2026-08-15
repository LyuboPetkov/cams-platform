package com.lyubo_learning.cams.repository;

import com.lyubo_learning.cams.entity.JobListingSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobListingSkillRepository extends JpaRepository<JobListingSkill, Long> {

    List<JobListingSkill> findByJobListingId(Long jobListingId);

}
