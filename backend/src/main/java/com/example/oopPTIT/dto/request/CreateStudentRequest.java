package com.example.oopPTIT.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** POST /api/admin/students. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateStudentRequest {

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
    private String studentCode;

    @NotBlank
    @Size(max = 255)
    private String fullName;

    @NotNull
    @PastOrPresent
    private LocalDate dateOfBirth;

    @NotBlank
    @Size(max = 255)
    private String phone;
}
