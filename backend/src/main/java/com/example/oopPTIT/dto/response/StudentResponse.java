package com.example.oopPTIT.dto.response;

import java.time.LocalDate;

public record StudentResponse(
        Long id,
        String studentCode,
        String fullName,
        LocalDate dateOfBirth,
        String phone
) {
}
