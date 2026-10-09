package com.example.oopPTIT.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** POST /api/sessions/{id}/attendance/open. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpenAttendanceSessionRequest {

    @NotNull
    @PositiveOrZero
    private Integer lateAfterMinutes;
}
