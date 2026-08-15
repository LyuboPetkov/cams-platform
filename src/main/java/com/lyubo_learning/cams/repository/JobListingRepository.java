package com.lyubo_learning.cams.repository;

import com.lyubo_learning.cams.entity.JobListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobListingRepository extends JpaRepository<JobListing, Long> {

    List<JobListing> findByCompanyId(Long companyId);

}
