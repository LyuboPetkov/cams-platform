package com.lyubo_learning.cams.mapper;

import com.lyubo_learning.cams.dto.EmployerRequestResponse;
import com.lyubo_learning.cams.entity.EmployerRequest;
import org.springframework.stereotype.Component;

@Component
public class EmployerRequestMapper {

    public EmployerRequestResponse toResponse(EmployerRequest entity) {
        return EmployerRequestResponse.builder()
                .id(entity.getId())
                .status(entity.getStatus())
                .companyName(entity.getCompanyName())
                .requestedAt(entity.getRequestedAt())
                .reviewedAt(entity.getReviewedAt())
                .rejectionReason(entity.getRejectionReason())
                .build();
    }
}
