package com.lyubo_learning.cams.service;

import com.lyubo_learning.cams.dto.SkillResponse;
import com.lyubo_learning.cams.entity.Skill;
import com.lyubo_learning.cams.mapper.SkillMapper;
import com.lyubo_learning.cams.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;
    private final SkillMapper mapper;

    public List<SkillResponse> search(String query, Integer limit) {
        Pageable pageable = (limit == null) ? Pageable.unpaged() : Pageable.ofSize(limit);

        List<Skill> skills =
                (query == null || query.isBlank())
                        ? skillRepository.findAll(pageable).getContent()
                        : skillRepository.findByNameContainingIgnoreCase(query.trim(), pageable);

        return skills.stream().map(mapper::toResponse).toList();
    }
}
