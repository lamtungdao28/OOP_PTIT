package com.example.oopPTIT.controller;

import org.springframework.security.access.prepost.PreAuthorize;

import com.example.oopPTIT.dto.request.CreateStudentRequest;
import com.example.oopPTIT.dto.request.UpdateStudentRequest;
import com.example.oopPTIT.dto.request.UpdateStudentStatusRequest;
import com.example.oopPTIT.dto.response.StudentDetailResponse;
import com.example.oopPTIT.dto.response.ResponsePage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

    private final StudentService studentService;

    @GetMapping("/{id}")
    public ResponseData<StudentDetailResponse> getStudent(@PathVariable("id") Long id) {
        return new ResponseData<>(HttpStatus.OK.value(), "Student retrieved successfully", studentService.getStudent(id));
    }

    @PatchMapping("/{id}")
    public ResponseData<StudentDetailResponse> updateStudent(@PathVariable("id") Long id,
            @Valid @RequestBody UpdateStudentRequest request) {
        return new ResponseData<>(HttpStatus.OK.value(), "Student updated successfully", studentService.updateStudent(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseData<StudentDetailResponse> updateStudentStatus(@PathVariable("id") Long id,
            @Valid @RequestBody UpdateStudentStatusRequest request) {
        return new ResponseData<>(HttpStatus.OK.value(), "Student status updated successfully",
                studentService.updateStudentStatus(id, request));
    }

    @PostMapping
    public ResponseEntity<ResponseData<StudentResponse>> createStudent(
            @Valid @RequestBody CreateStudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseData<>(HttpStatus.CREATED.value(), "Student created successfully",
                        studentService.createStudent(request)));
    }

    @GetMapping
    public ResponseData<ResponsePage<List<StudentResponse>>> searchStudents(
            @PageableDefault(size = 20, sort = "id") Pageable pageable,
            @RequestParam(name = "student", required = false) String[] student) {
        return new ResponseData<>(HttpStatus.OK.value(), "Students retrieved successfully",
                studentService.advanceSearchWithSpecification(pageable, student));
    }
}
