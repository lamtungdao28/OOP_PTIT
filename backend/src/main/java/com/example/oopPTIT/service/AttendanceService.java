package com.example.oopPTIT.service;

import com.example.oopPTIT.dto.request.ManualAttendanceRequest;
import com.example.oopPTIT.dto.request.OpenAttendanceSessionRequest;
import com.example.oopPTIT.dto.request.RecordAttendanceRequest;
import com.example.oopPTIT.dto.request.UpdateAttendanceRequest;
import com.example.oopPTIT.dto.response.AttendanceResponse;
import com.example.oopPTIT.dto.response.AttendanceResultResponse;
import com.example.oopPTIT.dto.response.AttendanceResultsResponse;
import com.example.oopPTIT.dto.response.AttendanceSessionResponse;
import com.example.oopPTIT.dto.response.AttendanceStatisticsResponse;
import com.example.oopPTIT.dto.response.ResponsePage;
import com.example.oopPTIT.entity.Attendance;
import com.example.oopPTIT.entity.AttendanceSession;
import com.example.oopPTIT.entity.ClassSession;
import com.example.oopPTIT.entity.Student;
import com.example.oopPTIT.repository.*;
import com.example.oopPTIT.security.CourseAccess;
import com.example.oopPTIT.security.CurrentUser;
import com.example.oopPTIT.util.SessionStatus;
import com.example.oopPTIT.util.Status;
import com.example.oopPTIT.util.TimeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AttendanceService {
    private static final int MANUAL_METHOD = 1;
    private static final int FACE_METHOD = 2;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final ClassSessionRepository classSessionRepository;
    private final CourseClassRepository courseClassRepository;
    private final StudentRepository studentRepository;
    private final StudentHasCourseClassRepository enrollmentRepository;
    private final FaceProfileRepository faceProfileRepository;
    private final CourseAccess courseAccess;
    private final CurrentUser currentUser;

    @Transactional
    @PreAuthorize("hasRole('LECTURER')")
    public AttendanceSessionResponse openAttendanceSession(Long classSessionId, OpenAttendanceSessionRequest request) {
        var classSession = getClassSession(classSessionId);
        lockAndRequireAssignedLecturer(classSession.getCourseClass().getId());
        if (sessionRepository.existsByClassSession_Id(classSessionId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Attendance session already exists for this class session");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime closesAt = classSession.getSessionDate().atTime(classSession.getEndTime());
        if (!closesAt.isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Class session has already ended");
        }
        AttendanceSession session = new AttendanceSession();
        session.setClassSession(classSession);
        session.setOpenedAt(now);
        session.setClosedAt(closesAt);
        session.setLateAfterMinutes(request.getLateAfterMinutes());
        session.setStatus(SessionStatus.OPEN);
        return toSessionResponse(sessionRepository.saveAndFlush(session), now);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('LECTURER')")
    public AttendanceSessionResponse getAttendanceSession(Long classSessionId) {
        var classSession = getClassSession(classSessionId);
        courseAccess.requireAssignedLecturer(classSession.getCourseClass());
        return toSessionResponse(getSessionForClassSession(classSessionId), LocalDateTime.now());
    }

    // The class lookup precedes its lock; READ_COMMITTED avoids an older MySQL snapshot after waiting.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('LECTURER')")
    public AttendanceResponse recordManualAttendance(Long classSessionId, ManualAttendanceRequest request) {
        var classSession = getClassSession(classSessionId);
        lockAndRequireAssignedLecturer(classSession.getCourseClass().getId());
        var session = getSessionForClassSession(classSessionId);
        return createManualAttendance(session, request.getStudentId(), request.getStatus());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('LECTURER')")
    public AttendanceResponse recordFaceAttendance(Long classSessionId, Long studentId, BigDecimal confidence,
                                                    String modelName, byte[] expectedEmbedding) {
        if (studentId == null || confidence == null || confidence.signum() < 0
                || confidence.compareTo(BigDecimal.ONE) > 0 || expectedEmbedding == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid face recognition result");
        }
        var classSession = getClassSession(classSessionId);
        Long classId = classSession.getCourseClass().getId();
        lockAndRequireAssignedLecturer(classId);
        var session = getSessionForClassSession(classSessionId);
        LocalDateTime now = LocalDateTime.now();
        if (!isOpenAt(session, now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Attendance session is closed");
        }
        if (enrollmentRepository.findActiveForUpdate(classId, studentId, Status.ACTIVE).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student is not enrolled in this class");
        }
        if (attendanceRepository.existsByAttendanceSession_IdAndStudent_Id(session.getId(), studentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Attendance already recorded for this student and session");
        }
        var profile = faceProfileRepository.findByStudentIdForUpdate(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Face profile is no longer active"));
        if (profile.getStatus() != Status.ACTIVE || profile.getStudent().getUser().getStatus() != Status.ACTIVE
                || !Objects.equals(modelName, profile.getModelName())
                || !Arrays.equals(expectedEmbedding, profile.getEmbedding())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Face profile changed during recognition");
        }
        int lateAfter = session.getLateAfterMinutes() == null ? 0 : session.getLateAfterMinutes();
        TimeStatus status = now.isAfter(session.getOpenedAt().plusMinutes(lateAfter))
                ? TimeStatus.LATE : TimeStatus.PRESENT;
        Attendance attendance = newAttendance(session, profile.getStudent(), status, now);
        attendance.setMethod(FACE_METHOD);
        attendance.setConfidence(confidence);
        return AttendanceResponse.from(attendanceRepository.saveAndFlush(attendance));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('LECTURER')")
    public AttendanceResponse updateAttendance(Long attendanceId, UpdateAttendanceRequest request) {
        Long classId = attendanceRepository.findCourseClassIdById(attendanceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance not found"));
        lockAndRequireAssignedLecturer(classId);
        // Load the editable record only after locking so dirty checking uses the latest status.
        var attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance not found"));
        attendance.setStatus(request.getStatus());
        return AttendanceResponse.from(attendanceRepository.saveAndFlush(attendance));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('LECTURER')")
    public AttendanceSessionResponse closeAttendanceSession(Long classSessionId) {
        var classSession = getClassSession(classSessionId);
        Long classId = classSession.getCourseClass().getId();
        lockAndRequireAssignedLecturer(classId);
        var session = getSessionForClassSession(classSessionId);
        LocalDateTime now = LocalDateTime.now();
        if (session.getStatus() == SessionStatus.CLOSED) {
            return toSessionResponse(session, now);
        }

        // Finalize even an expired session, without extending its original deadline.
        LocalDateTime closedAt = now.isBefore(session.getClosedAt()) ? now : session.getClosedAt();
        var missingStudents = enrollmentRepository.findStudentsWithoutAttendance(classId, session.getId(), Status.ACTIVE);
        var absences = missingStudents.stream()
                .map(student -> newAttendance(session, student, TimeStatus.ABSENT, closedAt)).toList();
        attendanceRepository.saveAll(absences);
        session.setStatus(SessionStatus.CLOSED);
        session.setClosedAt(closedAt);
        return toSessionResponse(sessionRepository.saveAndFlush(session), now);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    @PreAuthorize("hasRole('LECTURER')")
    public AttendanceResultsResponse getAttendanceResults(Long classSessionId, Pageable pageable) {
        var classSession = getClassSession(classSessionId);
        courseAccess.requireAssignedLecturer(classSession.getCourseClass());
        Set<String> fields = Set.of("id", "student.id", "student.studentCode", "student.fullName", "status",
                "recognizedAt", "method", "createdAt", "updatedAt");
        if (pageable.getSort().stream().anyMatch(order -> !fields.contains(order.getProperty()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported attendance result sort field");
        }
        var session = getSessionForClassSession(classSessionId);
        var page = attendanceRepository.findByAttendanceSession_Id(session.getId(), pageable).map(AttendanceResultResponse::from);
        var counts = new EnumMap<TimeStatus, Long>(TimeStatus.class);
        attendanceRepository.countByStatusForSession(session.getId())
                .forEach(count -> counts.put(count.getStatus(), count.getTotal()));
        long present = counts.getOrDefault(TimeStatus.PRESENT, 0L);
        long late = counts.getOrDefault(TimeStatus.LATE, 0L);
        long absent = counts.getOrDefault(TimeStatus.ABSENT, 0L);
        long unmarked = enrollmentRepository.countStudentsWithoutAttendance(classSession.getCourseClass().getId(),
                session.getId(), Status.ACTIVE);
        var statistics = new AttendanceStatisticsResponse(present + late + absent, present, late, absent, unmarked);
        var attendancePage = ResponsePage.<List<AttendanceResultResponse>>builder()
                .pageNo(page.getNumber()).pageSize(page.getSize()).totalPage(page.getTotalPages())
                .items(page.getContent()).build();
        return new AttendanceResultsResponse(toSessionResponse(session, LocalDateTime.now()), statistics, attendancePage);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER') and @courseAccess.canManage(#classId)")
    public AttendanceResponse recordAttendance(Long classId, RecordAttendanceRequest request) {
        lockAndRequireAssignedLecturer(classId);
        var session = sessionRepository.findById(request.attendanceSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance session not found"));
        if (!session.getClassSession().getCourseClass().getId().equals(classId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance session not found in this class");
        }
        return createManualAttendance(session, request.studentId(), request.status());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponsePage<List<AttendanceResponse>> getMyAttendance(Pageable pageable) {
        Set<String> fields = Set.of("id", "recognizedAt", "status");
        if (pageable.getSort().stream().anyMatch(order -> !fields.contains(order.getProperty()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported attendance sort field");
        }
        var page = attendanceRepository.findByStudent_User_Id(currentUser.id(), pageable).map(AttendanceResponse::from);
        return ResponsePage.<List<AttendanceResponse>>builder().pageNo(page.getNumber()).pageSize(page.getSize())
                .totalPage(page.getTotalPages()).items(page.getContent()).build();
    }

    private ClassSession getClassSession(Long classSessionId) {
        return classSessionRepository.findById(classSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class session not found"));
    }

    private AttendanceSession getSessionForClassSession(Long classSessionId) {
        return sessionRepository.findByClassSession_Id(classSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance session has not been opened"));
    }

    private void lockAndRequireAssignedLecturer(Long classId) {
        // All attendance writes serialize on the same class and check its current assignment.
        var courseClass = courseClassRepository.findByIdForUpdate(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course class not found"));
        courseAccess.requireAssignedLecturer(courseClass);
    }

    private AttendanceResponse createManualAttendance(AttendanceSession session, Long studentId, TimeStatus status) {
        LocalDateTime now = LocalDateTime.now();
        if (!isOpenAt(session, now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Attendance session is closed");
        }
        Long classId = session.getClassSession().getCourseClass().getId();
        if (!enrollmentRepository.existsByCourseClass_IdAndStudent_IdAndStatus(classId, studentId, Status.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student is not enrolled in this class");
        }
        if (attendanceRepository.existsByAttendanceSession_IdAndStudent_Id(session.getId(), studentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Attendance already recorded for this student and session");
        }
        var student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        return AttendanceResponse.from(attendanceRepository.saveAndFlush(newAttendance(session, student, status, now)));
    }

    private Attendance newAttendance(AttendanceSession session, Student student, TimeStatus status, LocalDateTime recordedAt) {
        Attendance attendance = new Attendance();
        attendance.setAttendanceSession(session);
        attendance.setStudent(student);
        attendance.setStatus(status);
        attendance.setRecognizedAt(recordedAt);
        attendance.setMethod(MANUAL_METHOD);
        attendance.setConfidence(BigDecimal.ZERO);
        return attendance;
    }

    private AttendanceSessionResponse toSessionResponse(AttendanceSession session, LocalDateTime now) {
        var classSession = session.getClassSession();
        SessionStatus status = isOpenAt(session, now) ? SessionStatus.OPEN : SessionStatus.CLOSED;
        return new AttendanceSessionResponse(session.getId(), classSession.getId(), classSession.getCourseClass().getId(),
                session.getOpenedAt(), session.getClosedAt(), session.getLateAfterMinutes(), status);
    }

    private boolean isOpenAt(AttendanceSession session, LocalDateTime now) {
        return session.getStatus() == SessionStatus.OPEN && now.isBefore(session.getClosedAt());
    }
}
