package com.example.oopPTIT.controller;

import org.springframework.security.access.prepost.PreAuthorize;

import com.example.oopPTIT.dto.request.CreateSemesterRequest;
import com.example.oopPTIT.dto.response.ResponseData;
import com.example.oopPTIT.dto.response.ResponsePage;
import com.example.oopPTIT.service.SemesterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/semesters")
@Validated
@RequiredArgsConstructor
public class AdminSemesterController {
    private final SemesterService semesterService;

    @PostMapping
    public ResponseData<Long> addSemester(@RequestBody @Valid CreateSemesterRequest request) {
        Long semesterId = semesterService.addSemester(request);
        return new ResponseData<>(HttpStatus.CREATED.value(), "added semester", semesterId);
    }

    @GetMapping
    public ResponseData<ResponsePage<?>> getSemesterWithSpecification(
            Pageable pageable,
            @RequestParam(required = false) String[] semester,
            @RequestParam(required = false) String search) {
        return new ResponseData<>(HttpStatus.ACCEPTED.value(), "Get all semester",
                semesterService.advanceSearchWithSpecification(pageable, semester, search));
    }
}
