package com.example.oopPTIT.dto.request;

import com.example.oopPTIT.util.Status;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** PATCH /api/admin/classes/{id}; null fields are left unchanged. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCourseClassRequest {

    @Pattern(regexp = "(?s).*\\S.*", message = "classCode must not be blank")
    @Size(max = 255)
    private String classCode;

    @Positive
    private Long subjectId;

    @Positive
    private Long semesterId;

    @Positive
    private Long lecturerId;

    @Positive
    private Integer maxStudents;

    private Status status;
}
