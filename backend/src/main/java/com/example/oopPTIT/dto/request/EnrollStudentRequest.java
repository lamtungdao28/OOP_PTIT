package com.example.oopPTIT.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** POST /api/admin/classes/{id}/enrollments. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollStudentRequest {

    @NotNull
    @Positive
    private Long studentId;
}
