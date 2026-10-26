package com.example.oopPTIT.controller;

import com.example.oopPTIT.dto.response.ResponseData;
import com.example.oopPTIT.dto.response.ResponsePage;
import com.example.oopPTIT.service.CourseClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@PreAuthorize("hasRole('STUDENT')")
@RequestMapping("/api/students/me/classes")
@Validated
@RequiredArgsConstructor
public class StudentCourseClassController {
    private final CourseClassService courseClassService;

    @GetMapping
    public ResponseData<ResponsePage<?>> getMyCourseClasses(Principal principal,
            @PageableDefault(size = 20, sort = "id") Pageable pageable,
            @RequestParam(name = "courseClass", required = false) String[] courseClass) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return new ResponseData<>(HttpStatus.OK.value(), "Enrolled course classes retrieved successfully",
                courseClassService.getEnrolledCourseClasses(principal.getName(), pageable, courseClass));
    }
}
