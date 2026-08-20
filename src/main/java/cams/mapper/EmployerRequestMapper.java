package cams.mapper;

import cams.dto.EmployerRequestResponse;
import cams.entity.EmployerRequest;
import org.springframework.stereotype.Component;

@Component
public class EmployerRequestMapper {

    public EmployerRequestResponse toResponse(EmployerRequest entity) {
        return EmployerRequestResponse.builder()
                .id(entity.getId())
                .status(entity.getStatus())
                .companyName(entity.getCompanyName())
                .companyDescription(entity.getCompanyDescription())
                .companyWebsite(entity.getCompanyWebsite())
                .companyLocation(entity.getCompanyLocation())
                .requesterFullName(entity.getUser().getFullName())
                .requesterEmail(entity.getUser().getEmail())
                .requestedAt(entity.getRequestedAt())
                .reviewedAt(entity.getReviewedAt())
                .rejectionReason(entity.getRejectionReason())
                .build();
    }
}
