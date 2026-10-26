package com.example.oopPTIT.dto.response;

import com.example.oopPTIT.entity.Attendance;
import com.example.oopPTIT.util.TimeStatus;

import java.time.LocalDateTime;

public record AttendanceResultResponse(Long id, Long studentId, String studentCode, String fullName,
                                       TimeStatus status, LocalDateTime recognizedAt, Integer method) {
    public static AttendanceResultResponse from(Attendance attendance) {
        var student = attendance.getStudent();
        return new AttendanceResultResponse(attendance.getId(), student.getId(), student.getStudentCode(),
                student.getFullName(), attendance.getStatus(), attendance.getRecognizedAt(), attendance.getMethod());
    }
}
