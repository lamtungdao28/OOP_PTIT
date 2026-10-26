package com.example.oopPTIT.controller;

import com.example.oopPTIT.dto.response.*;
import com.example.oopPTIT.service.AttendanceService;
import com.example.oopPTIT.service.StudentSelfService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {
    private final StudentSelfService studentService;
    private final AttendanceService attendanceService;

    @GetMapping("/profile")
    public ResponseData<StudentDetailResponse> profile() {
        return new ResponseData<>(200, "Student profile retrieved successfully", studentService.getProfile());
    }

    @GetMapping("/attendance")
    public ResponseData<ResponsePage<List<AttendanceResponse>>> attendance(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return new ResponseData<>(200, "Attendance retrieved successfully", attendanceService.getMyAttendance(pageable));
    }
}
