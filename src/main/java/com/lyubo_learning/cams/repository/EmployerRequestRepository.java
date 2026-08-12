package com.lyubo_learning.cams.repository;

import com.lyubo_learning.cams.entity.EmployerRequest;
import com.lyubo_learning.cams.entity.EmployerRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployerRequestRepository extends JpaRepository<EmployerRequest, Long> {

    Optional<EmployerRequest> findFirstByUserIdOrderByRequestedAtDesc(Long userId);

    List<EmployerRequest> findByStatus(EmployerRequestStatus status);

}
