package com.lyubo_learning.cams.repository;

import com.lyubo_learning.cams.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByNameIgnoreCase(String name);

}
