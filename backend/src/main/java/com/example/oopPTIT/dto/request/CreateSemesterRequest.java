package com.example.oopPTIT.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request body for POST /api/admin/semesters. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateSemesterRequest {

    @NotBlank
    @Size(max = 255)
    private String name;
}
