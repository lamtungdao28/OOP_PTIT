package com.example.oopPTIT.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** POST /api/admin/lecturers. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateLecturerRequest {

    @NotBlank
    @Size(max = 255)
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(max = 255)
    private String lecturerCode;

    @NotBlank
    @Size(max = 255)
    private String fullName;

    @NotBlank
    @Size(max = 255)
    private String department;

    @NotBlank
    @Size(max = 255)
    private String phone;
}
