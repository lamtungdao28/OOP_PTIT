package com.example.oopPTIT.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request body for PATCH /api/admin/classes/{id}/lecturer. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignLecturerRequest {

    @NotNull
    @Positive
    private Long lecturerId;
}
