package com.example.oopPTIT.dto.request;

import com.example.oopPTIT.util.Status;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** PATCH /api/admin/students/{id}/status; updates the linked user's status. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStudentStatusRequest {

    @NotNull
    private Status status;
}
