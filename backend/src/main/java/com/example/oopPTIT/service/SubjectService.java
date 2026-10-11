package com.example.oopPTIT.service;

import com.example.oopPTIT.dto.request.CreateSubjectRequest;
import com.example.oopPTIT.dto.request.UpdateSubjectRequest;
import com.example.oopPTIT.dto.response.ResponseData;
import com.example.oopPTIT.dto.response.ResponsePage;
import com.example.oopPTIT.entity.Subject;
import com.example.oopPTIT.repository.SubjectRepository;
import com.example.oopPTIT.repository.specification.SubjectSpecificationBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
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

    public void updatedSubject(Long id, UpdateSubjectRequest request){
        Subject subject = subjectRepository.findById(id).orElseThrow();
        subject.setSubjectCode(request.getSubjectCode());
        subject.setSubjectName(request.getSubjectName());
        subject.setCredits(request.getCredits());
        subject.setStatus(request.getStatus());
        subjectRepository.save(subject);
    }

    public ResponsePage<?> advanceSearchWithSpecification(Pageable pageable, String[] subject){
        Page<Subject> subjects = null;
        Specification<Subject> specification = Specification.unrestricted();
        List<Subject> list = new ArrayList<>();
        if(subject != null){
            SubjectSpecificationBuilder builder = new SubjectSpecificationBuilder();
            for (String s : subject){
               Pattern pattern = Pattern.compile("(\\w+?)([<:>~!])(.*)(\\p{Punct}?)(\\p{Punct}?)");
                Matcher matcher = pattern.matcher(s);
                if(matcher.find()){
                    builder.with(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4), matcher.group(5));
                }
            }
            list = subjectRepository.findAll(builder.build());
            return ResponsePage.builder()
                    .pageNo(pageable.getPageNumber())
                    .pageSize(pageable.getPageSize())
                    .totalPage(10)
                    .items(list)
                    .build();
        }
        else {
            subjects = subjectRepository.findAll(pageable);
        }
        return ResponsePage.builder()
                .pageNo(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalPage(subjects.getTotalPages())
                .items(subjects.stream().toList()).build();
    }
}
