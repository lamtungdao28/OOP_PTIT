package com.example.oopPTIT.dto.request;

import com.example.oopPTIT.util.Status;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** PATCH /api/attendance/{id}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAttendanceRequest {

    @NotNull
    private Status status;
}
