package com.example.oopPTIT.service;

import org.springframework.security.access.prepost.PreAuthorize;

import com.example.oopPTIT.dto.request.CreateSubjectRequest;
import com.example.oopPTIT.dto.request.UpdateSubjectRequest;
import com.example.oopPTIT.dto.response.ResponsePage;
import com.example.oopPTIT.dto.response.SubjectResponse;
import com.example.oopPTIT.entity.Subject;
import com.example.oopPTIT.repository.SubjectRepository;
import com.example.oopPTIT.repository.specification.SubjectSpecificationBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
@Getter
@RequiredArgsConstructor
public class SubjectService {
    private final SubjectRepository subjectRepository;
    public Long addSubject(CreateSubjectRequest request){
        Subject subject = Subject.builder()
                .subjectCode(request.getSubjectCode())
                .subjectName(request.getSubjectName())
                .credits(request.getCredits())
                .build();
        subjectRepository.save(subject);
        return subject.getId();
    }

    @Transactional
    public void updatedSubject(Long id, UpdateSubjectRequest request){
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found: " + id));
        if (request.getSubjectCode() != null) {
            subject.setSubjectCode(request.getSubjectCode());
        }
        if (request.getSubjectName() != null) {
            subject.setSubjectName(request.getSubjectName());
        }
        if (request.getCredits() != null) {
            subject.setCredits(request.getCredits());
        }
        if (request.getStatus() != null) {
            subject.setStatus(request.getStatus());
        }
        subjectRepository.save(subject);
    }

    public ResponsePage<?> advanceSearchWithSpecification(Pageable pageable, String[] subject){
        Specification<Subject> specification = Specification.unrestricted();
        if(subject != null){
            SubjectSpecificationBuilder builder = new SubjectSpecificationBuilder();
            for (String s : subject){
               Pattern pattern = Pattern.compile("(\\w+?)([<:>~!])(.*)(\\p{Punct}?)(\\p{Punct}?)");
                Matcher matcher = pattern.matcher(s);
                if(matcher.find()){
                    builder.with(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4), matcher.group(5));
                }
            }
            Specification<Subject> filters = builder.build();
            if (filters != null) {
                specification = filters;
            }
        }
        Page<SubjectResponse> subjects = subjectRepository.findAll(specification, pageable)
                .map(item -> SubjectResponse.builder()
                        .id(item.getId())
                        .subjectCode(item.getSubjectCode())
                        .subjectName(item.getSubjectName())
                        .credits(item.getCredits())
                        .status(item.getStatus())
                        .createdAt(item.getCreatedAt())
                        .updatedAt(item.getUpdatedAt())
                        .build());
        return ResponsePage.builder()
                .pageNo(subjects.getNumber())
                .pageSize(subjects.getSize())
                .totalPage(subjects.getTotalPages())
                .items(subjects.getContent())
                .build();
    }
}
