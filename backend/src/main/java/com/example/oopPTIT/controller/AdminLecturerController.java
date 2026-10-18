package com.example.oopPTIT.controller;

import com.example.oopPTIT.dto.request.CreateLecturerRequest;
import com.example.oopPTIT.dto.request.UpdateLecturerRequest;
import com.example.oopPTIT.dto.response.LecturerResponse;
import com.example.oopPTIT.dto.response.ResponseData;
import com.example.oopPTIT.dto.response.ResponsePage;
import com.example.oopPTIT.service.LecturerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/lecturers")
@RequiredArgsConstructor
public class AdminLecturerController {
    private final LecturerService lecturerService;

    @GetMapping
    public ResponseData<ResponsePage<List<LecturerResponse>>> getLecturers(
            @PageableDefault(size = 20, sort = "id") Pageable pageable,
            @RequestParam(name = "lecturer", required = false) String[] lecturer) {
        return new ResponseData<>("200", "Lecturers retrieved successfully",
                lecturerService.advanceSearchWithSpecification(pageable, lecturer));
    }

    @GetMapping("/{id}")
    public ResponseData<LecturerResponse> getLecturer(@PathVariable("id") Long id) {
        return new ResponseData<>("200", "Lecturer retrieved successfully", lecturerService.getLecturer(id));
    }

    @PostMapping
    public ResponseEntity<ResponseData<LecturerResponse>> createLecturer(
            @Valid @RequestBody CreateLecturerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseData<>("201",
                "Lecturer created successfully", lecturerService.createLecturer(request)));
    }

    @PatchMapping("/{id}")
    public ResponseData<LecturerResponse> updateLecturer(@PathVariable("id") Long id,
            @Valid @RequestBody UpdateLecturerRequest request) {
        return new ResponseData<>("200", "Lecturer updated successfully", lecturerService.updateLecturer(id, request));
    }
}
