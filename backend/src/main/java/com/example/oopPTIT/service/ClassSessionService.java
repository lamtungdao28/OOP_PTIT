package com.example.oopPTIT.service;

import com.example.oopPTIT.dto.response.ClassSessionResponse;
import com.example.oopPTIT.dto.response.ResponsePage;
import com.example.oopPTIT.repository.ClassSessionRepository;
import com.example.oopPTIT.repository.CourseClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClassSessionService {
    private final ClassSessionRepository classSessionRepository;
    private final CourseClassRepository courseClassRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER') and @courseAccess.canManage(#classId)")
    public ResponsePage<List<ClassSessionResponse>> getSessions(Long classId, Pageable pageable) {
        Set<String> sortFields = Set.of("id", "sessionNo", "sessionDate", "startTime", "endTime", "createdAt", "updatedAt");
        if (pageable.getSort().stream().anyMatch(order -> !sortFields.contains(order.getProperty()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported class session sort field");
        }
        if (!courseClassRepository.existsById(classId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course class not found: " + classId);
        }
        var sessions = classSessionRepository.findByCourseClass_Id(classId, pageable).map(ClassSessionResponse::from);
        return ResponsePage.<List<ClassSessionResponse>>builder()
                .pageNo(sessions.getNumber())
                .pageSize(sessions.getSize())
                .totalPage(sessions.getTotalPages())
                .items(sessions.getContent())
                .build();
    }
}
