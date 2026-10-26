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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/classes")
@Validated
@RequiredArgsConstructor
public class CourseClassController {
    private final CourseClassService courseClassService;

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER') and @courseAccess.canManage(#id)")
    public ResponseData<ResponsePage<?>> getStudentsInCourseClass(@PathVariable("id") Long id,
            @PageableDefault(size = 20, sort = "id") Pageable pageable,
            @RequestParam(name = "student", required = false) String[] student) {
        return new ResponseData<>(HttpStatus.OK.value(), "Course class students retrieved successfully",
                courseClassService.getStudentsInCourseClass(id, pageable, student));
    }

}
