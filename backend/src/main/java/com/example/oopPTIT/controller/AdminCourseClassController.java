package com.example.oopPTIT.controller;

import com.example.oopPTIT.dto.request.CreateCourseClassRequest;
import com.example.oopPTIT.dto.response.CourseClassResponse;
import com.example.oopPTIT.dto.response.ResponseData;
import com.example.oopPTIT.dto.response.ResponseError;
import com.example.oopPTIT.dto.response.ResponsePage;
import com.example.oopPTIT.service.CourseClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/classes")
@Validated
@RequiredArgsConstructor
public class AdminCourseClassController {
    private final CourseClassService courseClassService;

    @PostMapping
    public ResponseData<Long> addCourseClass(@RequestBody @Valid CreateCourseClassRequest request) {
        try {
            Long courseClassId = courseClassService.addCourseClass(request);
            return new ResponseData<>(HttpStatus.CREATED.value(), "added course class", courseClassId);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return new ResponseError(HttpStatus.BAD_REQUEST.value(), "error to add new course class");
        }
    }

    @GetMapping
    public ResponseData<ResponsePage<?>> getCourseClassWithSpecification(
            Pageable pageable, @RequestParam(required = false) String[] courseClass) {
        return new ResponseData<>(HttpStatus.ACCEPTED.value(), "Get all course class",
                courseClassService.advanceSearchWithSpecification(pageable, courseClass));
    }

    @GetMapping("/{id}")
    public ResponseData<CourseClassResponse> getCourseClassById(@PathVariable Long id) {
        return new ResponseData<>(HttpStatus.ACCEPTED.value(), "Get course class detail",
                courseClassService.getCourseClassById(id));
    }
}
