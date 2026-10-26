package com.example.oopPTIT.dto.response;

import com.example.oopPTIT.entity.Attendance;
import com.example.oopPTIT.util.TimeStatus;

import java.time.LocalDateTime;

public record AttendanceResponse(Long id, Long classId, Long attendanceSessionId, Long studentId,
                                 TimeStatus status, LocalDateTime recognizedAt, Integer method) {
    public static AttendanceResponse from(Attendance attendance) {
        var session = attendance.getAttendanceSession();
        return new AttendanceResponse(attendance.getId(), session.getClassSession().getCourseClass().getId(),
                session.getId(), attendance.getStudent().getId(), attendance.getStatus(),
                attendance.getRecognizedAt(), attendance.getMethod());
    }
}
