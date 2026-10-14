package com.example.oopPTIT.controller;

import com.example.oopPTIT.dto.request.CreateCourseClassRequest;
import com.example.oopPTIT.dto.response.ResponseData;
import com.example.oopPTIT.dto.response.ResponseError;
import com.example.oopPTIT.service.CourseClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
