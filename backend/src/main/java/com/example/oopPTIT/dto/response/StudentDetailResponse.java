package com.example.oopPTIT.dto.response;

import com.example.oopPTIT.entity.Student;
import com.example.oopPTIT.util.Status;
import java.time.LocalDate;

public record StudentDetailResponse(Long id, String studentCode, String fullName,
                                    LocalDate dateOfBirth, String phone,
                                    String username, String email, Status status) {
    public static StudentDetailResponse from(Student student) {
        var user = student.getUser();
        return new StudentDetailResponse(student.getId(), student.getStudentCode(), student.getFullName(),
                student.getDateOfBirth(), student.getPhone(), user.getUsername(), user.getEmail(), user.getStatus());
    }
}
