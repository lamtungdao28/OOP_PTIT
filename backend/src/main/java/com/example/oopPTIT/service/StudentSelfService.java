package com.example.oopPTIT.service;

import com.example.oopPTIT.dto.response.StudentDetailResponse;
import com.example.oopPTIT.exception.ResourceNotFoundException;
import com.example.oopPTIT.repository.StudentRepository;
import com.example.oopPTIT.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentSelfService {
    private final StudentRepository studentRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public StudentDetailResponse getProfile() {
        return studentRepository.findByUser_Id(currentUser.id()).map(StudentDetailResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
    }
}
