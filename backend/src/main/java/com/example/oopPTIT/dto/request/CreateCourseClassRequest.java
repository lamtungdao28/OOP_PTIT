package com.example.oopPTIT.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request body for POST /api/admin/classes. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCourseClassRequest {

    @NotBlank
    @Size(max = 255)
    private String classCode;

    @NotNull
    @Positive
    private Long subjectId;

    @NotNull
    @Positive
    private Long semesterId;

    @NotNull
    @Positive
    private Long lecturerId;

    @NotNull
    @Positive
    private Integer maxStudents;
}
