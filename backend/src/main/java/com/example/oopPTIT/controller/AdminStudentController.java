package com.example.oopPTIT.controller;

import com.example.oopPTIT.dto.request.CreateStudentRequest;
import com.example.oopPTIT.dto.request.UpdateStudentRequest;
import com.example.oopPTIT.dto.request.UpdateStudentStatusRequest;
import com.example.oopPTIT.dto.response.StudentDetailResponse;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.example.oopPTIT.dto.response.ResponseData;
import com.example.oopPTIT.dto.response.StudentResponse;
import com.example.oopPTIT.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

    private final StudentService studentService;

    @GetMapping("/{id}")
    public ResponseData<StudentDetailResponse> getStudent(@PathVariable("id") Long id) {
        return new ResponseData<>("200", "Student retrieved successfully", studentService.getStudent(id));
    }

    @PatchMapping("/{id}")
    public ResponseData<StudentDetailResponse> updateStudent(@PathVariable("id") Long id,
            @Valid @RequestBody UpdateStudentRequest request) {
        return new ResponseData<>("200", "Student updated successfully", studentService.updateStudent(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseData<StudentDetailResponse> updateStudentStatus(@PathVariable("id") Long id,
            @Valid @RequestBody UpdateStudentStatusRequest request) {
        return new ResponseData<>("200", "Student status updated successfully",
                studentService.updateStudentStatus(id, request));
    }

    @PostMapping
    public ResponseEntity<ResponseData<StudentResponse>> createStudent(
            @Valid @RequestBody CreateStudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseData<>("201", "Student created successfully",
                        studentService.createStudent(request)));
    }

    @GetMapping
    public ResponseData<List<StudentResponse>> searchStudents(
            @RequestParam(name = "fullName", required = false) String fullName,
            @RequestParam(name = "studentCode", required = false) String studentCode) {
        return new ResponseData<>("200", "Students retrieved successfully",
                studentService.searchStudents(fullName, studentCode));
    }
}
