package com.example.oopPTIT.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request body for POST /api/admin/subjects. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubjectRequest {

    @NotBlank
    @Size(max = 255)
    private String subjectCode;

    @NotBlank
    @Size(max = 255)
    private String subjectName;

    @NotNull
    @Positive
    private Integer credits;
}
