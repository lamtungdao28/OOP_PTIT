package com.example.oopPTIT.dto.response;

import com.example.oopPTIT.util.SessionStatus;

import java.time.LocalDateTime;

public record AttendanceSessionResponse(Long id, Long classSessionId, Long classId,
                                        LocalDateTime openedAt, LocalDateTime closedAt,
                                        Integer lateAfterMinutes, SessionStatus status) {
}
