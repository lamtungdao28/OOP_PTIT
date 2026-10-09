package com.example.oopPTIT.dto.request;

import com.example.oopPTIT.util.Status;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request body for PATCH /api/admin/subjects/{id}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSubjectRequest {

    @Size(max = 255)
    @Pattern(regexp = "(?s).*\\S.*", message = "subjectCode must not be blank")
    private String subjectCode;

    @Size(max = 255)
    @Pattern(regexp = "(?s).*\\S.*", message = "subjectName must not be blank")
    private String subjectName;

    @Positive
    private Integer credits;

    private Status status;
}
