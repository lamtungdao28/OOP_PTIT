package com.example.oopPTIT.dto.response;

import com.example.oopPTIT.entity.Lecturer;
import com.example.oopPTIT.util.Status;

public record LecturerResponse(Long id, String lecturerCode, String fullName, String department,
                               String phone, String username, String email, Status status) {
    public static LecturerResponse from(Lecturer lecturer) {
        var user = lecturer.getUser();
        return new LecturerResponse(lecturer.getId(), lecturer.getLecturerCode(), lecturer.getFullName(),
                lecturer.getDepartment(), lecturer.getPhone(), user.getUsername(), user.getEmail(), user.getStatus());
    }
}
