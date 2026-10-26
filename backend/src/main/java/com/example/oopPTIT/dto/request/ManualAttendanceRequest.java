package com.example.oopPTIT.dto.request;

import com.example.oopPTIT.util.TimeStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ManualAttendanceRequest {

    @NotNull
    @Positive
    private Long studentId;

    @NotNull
    private TimeStatus status;
}