package com.lyubo_learning.cams.service;

import com.lyubo_learning.cams.dto.CompanyResponse;
import com.lyubo_learning.cams.dto.CompanyUpdateRequest;
import com.lyubo_learning.cams.entity.Company;
import com.lyubo_learning.cams.entity.EmployerRequest;
import com.lyubo_learning.cams.entity.User;
import com.lyubo_learning.cams.exception.CompanyNameAlreadyExistsException;
import com.lyubo_learning.cams.exception.ResourceNotFoundException;
import com.lyubo_learning.cams.mapper.CompanyMapper;
import com.lyubo_learning.cams.repository.CompanyRepository;
import com.lyubo_learning.cams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final CompanyMapper mapper;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public Company createFromEmployerRequest(EmployerRequest request) {
        String normalisedName = request.getCompanyName().trim().replaceAll("\\s+", " ");

        if (companyRepository.existsByNameIgnoreCase(normalisedName)) {
            throw new CompanyNameAlreadyExistsException("A company named '" + normalisedName + "' already exists");
        }

        Company company = Company.builder()
                .name(normalisedName)
                .description(request.getCompanyDescription())
                .website(request.getCompanyWebsite())
                .location(request.getCompanyLocation())
                .build();

        return companyRepository.save(company);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        return mapper.toResponse(company);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getMyCompany() {
        Company company = getAuthenticatedUser().getCompany();

        if (company == null) {
            throw new ResourceNotFoundException("No company found for this user");
        }

        return mapper.toResponse(company);
    }

    @Transactional
    public CompanyResponse updateMyCompany(CompanyUpdateRequest request) {
        Company company = getAuthenticatedUser().getCompany();

        if (company == null) {
            throw new ResourceNotFoundException("No company found for this user");
        }

        if (request.getDescription() != null) {
            company.setDescription(request.getDescription());
        }
        if (request.getWebsite() != null) {
            company.setWebsite(request.getWebsite());
        }
        if (request.getLocation() != null) {
            company.setLocation(request.getLocation());
        }
        if (request.getLogoUrl() != null) {
            company.setLogoUrl(request.getLogoUrl());
        }

        Company saved = companyRepository.save(company);
        return mapper.toResponse(saved);
    }

}
