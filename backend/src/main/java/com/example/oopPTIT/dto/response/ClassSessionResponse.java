package com.example.oopPTIT.dto.response;

import com.example.oopPTIT.entity.ClassSession;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ClassSessionResponse(Long id, Long classId, Integer sessionNo, LocalDate sessionDate,
                                   LocalTime startTime, LocalTime endTime,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static ClassSessionResponse from(ClassSession session) {
        return new ClassSessionResponse(session.getId(), session.getCourseClass().getId(), session.getSessionNo(),
                session.getSessionDate(), session.getStartTime(), session.getEndTime(),
                session.getCreatedAt(), session.getUpdatedAt());
    }
}
