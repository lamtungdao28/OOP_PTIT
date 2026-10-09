package com.example.oopPTIT.dto.request;

import com.example.oopPTIT.util.Status;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** POST /api/sessions/{id}/attendance/manual; the backend sets method to MANUAL. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ManualAttendanceRequest {

    @NotNull
    @Positive
    private Long studentId;

    @NotNull
    private Status status;
}
