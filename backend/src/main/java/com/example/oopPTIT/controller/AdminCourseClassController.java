package com.example.oopPTIT.controller;

import org.springframework.security.access.prepost.PreAuthorize;

import com.example.oopPTIT.dto.request.AssignLecturerRequest;
import com.example.oopPTIT.dto.request.CreateCourseClassRequest;
import com.example.oopPTIT.dto.request.EnrollStudentRequest;
import com.example.oopPTIT.dto.request.UpdateCourseClassRequest;
import com.example.oopPTIT.dto.response.CourseClassResponse;
import com.example.oopPTIT.dto.response.ResponseData;
import com.example.oopPTIT.dto.response.ResponsePage;
import com.example.oopPTIT.service.CourseClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/classes")
@Validated
@RequiredArgsConstructor
public class AdminCourseClassController {
    private final CourseClassService courseClassService;

    @PostMapping
    public ResponseData<Long> addCourseClass(@RequestBody @Valid CreateCourseClassRequest request) {
        Long courseClassId = courseClassService.addCourseClass(request);
        return new ResponseData<>(HttpStatus.CREATED.value(), "added course class", courseClassId);
    }

    @GetMapping
    public ResponseData<ResponsePage<?>> getCourseClassWithSpecification(
            Pageable pageable, @RequestParam(required = false) String[] courseClass) {
        return new ResponseData<>(HttpStatus.ACCEPTED.value(), "Get all course class",
                courseClassService.findAllCourseClasses(pageable, courseClass));
    }

    @GetMapping("/{id}")
    public ResponseData<CourseClassResponse> getCourseClassById(@PathVariable Long id) {
        return new ResponseData<>(HttpStatus.ACCEPTED.value(), "Get course class detail",
                courseClassService.getCourseClassById(id));
    }

    @PatchMapping("/{id}")
    public ResponseData<CourseClassResponse> updateCourseClass(@PathVariable Long id,
            @RequestBody @Valid UpdateCourseClassRequest request) {
        return new ResponseData<>(HttpStatus.OK.value(), "Course class updated successfully",
                courseClassService.updateCourseClass(id, request));
    }

    @PatchMapping("/{id}/lecturer")
    public ResponseData<CourseClassResponse> assignLecturer(@PathVariable("id") Long id,
            @RequestBody @Valid AssignLecturerRequest request) {
        return new ResponseData<>(HttpStatus.OK.value(), "Course class lecturer updated successfully",
                courseClassService.assignLecturer(id, request));
    }

    @PostMapping("/{id}/enrollments")
    public ResponseData<Long> addStudentToClass(@PathVariable("id") Long id,
                                                @RequestBody @Valid EnrollStudentRequest request){
        return new ResponseData<>(HttpStatus.OK.value(), "Course  class added student",
                courseClassService.addStudentToClass(id,request));
    }

    @DeleteMapping("/{id}/enrollments/{studentId}")
    public ResponseData<Void> removeStudentFromClass(@PathVariable("id") Long id,
            @PathVariable("studentId") Long studentId) {
        courseClassService.removeStudentFromClass(id, studentId);
        return new ResponseData<>(HttpStatus.OK.value(), "Student removed from course class successfully");
    }

}
