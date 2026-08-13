package com.lyubo_learning.cams.mapper;

import com.lyubo_learning.cams.dto.CompanyResponse;
import com.lyubo_learning.cams.entity.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanyResponse toResponse(Company entity) {
        return CompanyResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .website(entity.getWebsite())
                .location(entity.getLocation())
                .logoUrl(entity.getLogoUrl())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
