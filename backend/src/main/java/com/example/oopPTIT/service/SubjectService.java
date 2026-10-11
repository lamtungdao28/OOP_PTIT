package com.example.oopPTIT.service;

import com.example.oopPTIT.dto.request.CreateSubjectRequest;
import com.example.oopPTIT.dto.request.UpdateSubjectRequest;
import com.example.oopPTIT.entity.Subject;
import com.example.oopPTIT.repository.SubjectRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
}
