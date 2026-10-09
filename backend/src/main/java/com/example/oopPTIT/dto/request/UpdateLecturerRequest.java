package com.example.oopPTIT.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** PATCH /api/admin/lecturers/{id}; null fields are left unchanged. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLecturerRequest {

    @Pattern(regexp = "(?s).*\\S.*", message = "lecturerCode must not be blank")
    @Size(max = 255)
    private String lecturerCode;

    @Pattern(regexp = "(?s).*\\S.*", message = "fullName must not be blank")
    @Size(max = 255)
    private String fullName;

    @Pattern(regexp = "(?s).*\\S.*", message = "department must not be blank")
    @Size(max = 255)
    private String department;

    @Pattern(regexp = "(?s).*\\S.*", message = "phone must not be blank")
    @Size(max = 255)
    private String phone;

    @Pattern(regexp = "(?s).*\\S.*", message = "email must not be blank")
    @Email
    @Size(max = 255)
    private String email;
}
