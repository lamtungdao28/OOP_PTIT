package com.example.oopPTIT.service;

import com.example.oopPTIT.entity.*;
import com.example.oopPTIT.repository.*;
import com.example.oopPTIT.util.*;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    public static final int AUTO = 1;
    public static final int MANUAL = 2;
    public static final int SYSTEM = 3;

    private final AttendanceRepository attendanceRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final ClassSessionRepository classSessionRepository;
    private final StudentHasCourseClassRepository enrollmentRepository;

    // ==============================
    // 1. Mở phiên điểm danh
    // ==============================

    @Transactional
    public SessionResult open(
            Long classSessionId,
            Integer lateAfterMinutes
    ) {

        if (lateAfterMinutes == null
                || lateAfterMinutes < 0
                || lateAfterMinutes > 180) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid lateAfterMinutes"
            );
        }

        ClassSession classSession = classSessionRepository
                .findById(classSessionId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Class session not found"
                        )
                );

        if (!LocalDate.now().equals(
                classSession.getSessionDate())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Session is not scheduled for today"
            );
        }

        LocalTime now = LocalTime.now();

        if (now.isBefore(
                classSession.getStartTime().minusMinutes(15)
        ) || now.isAfter(classSession.getEndTime())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Outside scheduled class time"
            );
        }

        if (sessionRepository.existsByClassSessionId(
                classSessionId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Attendance session already exists"
            );
        }

        AttendanceSession session = new AttendanceSession();

        session.setClassSession(classSession);
        session.setOpenedAt(LocalDateTime.now());
        session.setClosedAt(null);
        session.setLateAfterMinutes(lateAfterMinutes);
        session.setStatus(SessionStatus.OPEN);

        sessionRepository.save(session);

        return new SessionResult(
                session.getId(),
                SessionStatus.OPEN,
                0
        );
    }

    // ==============================
    // 2. Ghi điểm danh bằng AI
    // ==============================

    @Transactional
    public MarkResult recordAutomatic(
            Long classSessionId,
            Long studentId,
            double similarity
    ) {

        AttendanceSession session =
                getLockedSession(classSessionId);

        if (session.getStatus() != SessionStatus.OPEN) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Attendance session is closed"
            );
        }

        Student student = getEnrolledStudent(
                session,
                studentId
        );

        var existing = attendanceRepository
                .findByAttendanceSessionIdAndStudentId(
                        session.getId(),
                        studentId
                );

        if (existing.isPresent()) {
            return new MarkResult(
                    studentId,
                    existing.get().getStatus(),
                    true
            );
        }

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime lateTime = session
                .getOpenedAt()
                .plusMinutes(
                        session.getLateAfterMinutes()
                );

        TimeStatus status =
                now.isAfter(lateTime)
                        ? TimeStatus.LATE
                        : TimeStatus.PRESENT;

        Attendance attendance = new Attendance();

        attendance.setAttendanceSession(session);
        attendance.setStudent(student);
        attendance.setRecognizedAt(now);
        attendance.setStatus(status);
        attendance.setMethod(AUTO);

        attendance.setConfidence(
                BigDecimal.valueOf(similarity)
                        .setScale(
                                4,
                                RoundingMode.HALF_UP
                        )
        );

        attendanceRepository.save(attendance);

        return new MarkResult(
                studentId,
                status,
                false
        );
    }

    // ==============================
    // 3. Điểm danh thủ công
    // ==============================

    @Transactional
    public MarkResult markManual(
            Long classSessionId,
            Long studentId,
            TimeStatus status
    ) {

        AttendanceSession session =
                getLockedSession(classSessionId);

        Student student = getEnrolledStudent(
                session,
                studentId
        );

        Attendance attendance = attendanceRepository
                .findByAttendanceSessionIdAndStudentId(
                        session.getId(),
                        studentId
                )
                .orElseGet(Attendance::new);

        attendance.setAttendanceSession(session);
        attendance.setStudent(student);
        attendance.setStatus(status);
        attendance.setMethod(MANUAL);
        attendance.setConfidence(null);

        attendance.setRecognizedAt(
                status == TimeStatus.ABSENT
                        ? null
                        : LocalDateTime.now()
        );

        attendanceRepository.save(attendance);

        return new MarkResult(
                studentId,
                status,
                false
        );
    }

    // ==============================
    // 4. Đóng phiên điểm danh
    // ==============================

    @Transactional
    public SessionResult close(Long classSessionId) {

        AttendanceSession session =
                getLockedSession(classSessionId);

        if (session.getStatus() == SessionStatus.CLOSED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Session already closed"
            );
        }

        Long courseClassId = session
                .getClassSession()
                .getCourseClass()
                .getId();

        List<StudentHasCourseClass> enrollments =
                enrollmentRepository
                        .findByCourseClassIdAndStatus(
                                courseClassId,
                                Status.ACTIVE
                        );

        int absentCount = 0;

        for (StudentHasCourseClass enrollment : enrollments) {

            Student student = enrollment.getStudent();

            if (student.getUser().getStatus()
                    != Status.ACTIVE) {
                continue;
            }

            boolean alreadyMarked = attendanceRepository
                    .findByAttendanceSessionIdAndStudentId(
                            session.getId(),
                            student.getId()
                    )
                    .isPresent();

            if (alreadyMarked) {
                continue;
            }

            Attendance absent = new Attendance();

            absent.setAttendanceSession(session);
            absent.setStudent(student);
            absent.setRecognizedAt(null);
            absent.setStatus(TimeStatus.ABSENT);
            absent.setMethod(SYSTEM);
            absent.setConfidence(null);

            attendanceRepository.save(absent);

            absentCount++;
        }

        session.setClosedAt(LocalDateTime.now());
        session.setStatus(SessionStatus.CLOSED);

        return new SessionResult(
                session.getId(),
                SessionStatus.CLOSED,
                absentCount
        );
    }

    // ==============================
    // Helper methods
    // ==============================

    private AttendanceSession getLockedSession(
            Long classSessionId
    ) {

        return sessionRepository
                .lockByClassSessionId(classSessionId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Attendance session not opened"
                        )
                );
    }

    private Student getEnrolledStudent(
            AttendanceSession session,
            Long studentId
    ) {

        Long courseClassId = session
                .getClassSession()
                .getCourseClass()
                .getId();

        Student student = enrollmentRepository
                .findByCourseClassIdAndStatus(
                        courseClassId,
                        Status.ACTIVE
                )
                .stream()
                .map(StudentHasCourseClass::getStudent)
                .filter(s ->
                        s.getId().equals(studentId)
                )
                .findFirst()
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Student is not enrolled"
                        )
                );

        if (student.getUser().getStatus() != Status.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Student account inactive"
            );
        }

        return student;
    }

    public record MarkResult(
            Long studentId,
            TimeStatus status,
            boolean alreadyMarked
    ) {}

    public record SessionResult(
            Long attendanceSessionId,
            SessionStatus status,
            int absencesCreated
    ) {}
}