package com.example.oopPTIT.service;

import com.example.oopPTIT.dto.request.CreateClassSessionRequest;
import com.example.oopPTIT.dto.response.ClassSessionResponse;
import com.example.oopPTIT.dto.response.ResponsePage;
import com.example.oopPTIT.entity.ClassSession;
import com.example.oopPTIT.repository.ClassSessionRepository;
import com.example.oopPTIT.repository.CourseClassRepository;
import com.example.oopPTIT.security.CourseAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClassSessionService {
    private static final int MAX_CREATE_ATTEMPTS = 3;
    private final ClassSessionRepository classSessionRepository;
    private final CourseClassRepository courseClassRepository;
    private final CourseAccess courseAccess;
    private final PlatformTransactionManager transactionManager;

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    public ClassSessionResponse createSession(Long classId, CreateClassSessionRequest request) {
        var transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        // session_no is globally unique; retry a concurrent allocation in a fresh transaction.
        for (int attempt = 1; ; attempt++) {
            try {
                return transaction.execute(status -> createSessionInTransaction(classId, request));
            } catch (DataIntegrityViolationException | CannotAcquireLockException exception) {
                if (attempt >= MAX_CREATE_ATTEMPTS) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Class session conflicts with existing data; retry the request", exception);
                }
            }
        }
    }

    private ClassSessionResponse createSessionInTransaction(Long classId, CreateClassSessionRequest request) {
        var courseClass = courseClassRepository.findByIdForUpdate(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Course class not found: " + classId));
        courseAccess.requireAssignedLecturer(courseClass);
        if (classSessionRepository.existsByCourseClass_IdAndSessionDateAndStartTimeAndEndTime(
                classId, request.getSessionDate(), request.getStartTime(), request.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Class session already exists for this time slot");
        }
        int lastSessionNo = classSessionRepository.findMaxSessionNo();
        if (lastSessionNo == Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No class session numbers are available");
        }
        ClassSession session = new ClassSession();
        session.setCourseClass(courseClass);
        session.setSessionNo(lastSessionNo + 1);
        session.setSessionDate(request.getSessionDate());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        return ClassSessionResponse.from(classSessionRepository.saveAndFlush(session));
    }

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
