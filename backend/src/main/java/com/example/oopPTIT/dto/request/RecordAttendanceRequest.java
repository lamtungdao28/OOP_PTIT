package com.example.oopPTIT.dto.request;

import com.example.oopPTIT.util.TimeStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RecordAttendanceRequest(@NotNull @Positive Long attendanceSessionId,
                                      @NotNull @Positive Long studentId,
                                      @NotNull TimeStatus status) {
}
