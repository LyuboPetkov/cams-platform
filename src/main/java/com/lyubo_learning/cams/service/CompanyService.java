package com.lyubo_learning.cams.service;

import com.lyubo_learning.cams.entity.Company;
import com.lyubo_learning.cams.entity.EmployerRequest;
import com.lyubo_learning.cams.exception.CompanyNameAlreadyExistsException;
import com.lyubo_learning.cams.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

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

}
