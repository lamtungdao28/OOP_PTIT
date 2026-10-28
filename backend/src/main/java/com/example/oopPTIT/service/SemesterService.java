package com.example.oopPTIT.service;

import org.springframework.security.access.prepost.PreAuthorize;

import com.example.oopPTIT.dto.request.CreateSemesterRequest;
import com.example.oopPTIT.dto.response.ResponsePage;
import com.example.oopPTIT.dto.response.SemesterResponse;
import com.example.oopPTIT.entity.Semester;
import com.example.oopPTIT.repository.SemesterRepository;
import com.example.oopPTIT.repository.specification.SemesterSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SemesterService {
    private static final Pattern SEARCH_PATTERN = Pattern.compile("(\\w+?)([<:>~!])(.*)(\\p{Punct}?)(\\p{Punct}?)");

    private final SemesterRepository semesterRepository;

    public Long addSemester(CreateSemesterRequest request) {
        Semester semester = new Semester();
        semester.setName(request.getName());
        return semesterRepository.save(semester).getId();
    }

    public ResponsePage<?> advanceSearchWithSpecification(Pageable pageable, String[] semester) {
        return advanceSearchWithSpecification(pageable, semester, null);
    }

    public ResponsePage<?> advanceSearchWithSpecification(Pageable pageable, String[] semester, String search) {
        Specification<Semester> specification = Specification.unrestricted();
        if (semester != null) {
            SemesterSpecificationBuilder builder = new SemesterSpecificationBuilder();
            for (String filter : semester) {
                Matcher matcher = SEARCH_PATTERN.matcher(filter);
                if (matcher.find()) {
                    builder.with(matcher.group(1), matcher.group(2), matcher.group(3),
                            matcher.group(4), matcher.group(5));
                }
            }
            Specification<Semester> filters = builder.build();
            if (filters != null) {
                specification = filters;
            }
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.strip().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern));
        }
        Page<SemesterResponse> semesters = semesterRepository.findAll(specification, pageable)
                .map(item -> SemesterResponse.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .createdAt(item.getCreatedAt())
                        .updatedAt(item.getUpdatedAt())
                        .build());
        return ResponsePage.builder()
                .pageNo(semesters.getNumber())
                .pageSize(semesters.getSize())
                .totalPage(semesters.getTotalPages())
                .items(semesters.getContent())
                .build();
    }
}
