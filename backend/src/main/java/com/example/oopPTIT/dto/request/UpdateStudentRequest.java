package com.example.oopPTIT.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** PATCH /api/admin/students/{id}; null fields are left unchanged. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStudentRequest {

    @Pattern(regexp = "(?s).*\\S.*", message = "studentCode must not be blank")
    @Size(max = 255)
    private String studentCode;

    @Pattern(regexp = "(?s).*\\S.*", message = "fullName must not be blank")
    @Size(max = 255)
    private String fullName;

    @PastOrPresent
    private LocalDate dateOfBirth;

    @Pattern(regexp = "(?s).*\\S.*", message = "phone must not be blank")
    @Size(max = 255)
    private String phone;

    @Pattern(regexp = "(?s).*\\S.*", message = "email must not be blank")
    @Email
    @Size(max = 255)
    private String email;
}
