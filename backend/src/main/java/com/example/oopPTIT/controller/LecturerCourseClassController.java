package com.example.oopPTIT.controller;

import com.example.oopPTIT.dto.request.ManualAttendanceRequest;
import com.example.oopPTIT.dto.request.OpenAttendanceSessionRequest;
import com.example.oopPTIT.dto.request.RecordAttendanceRequest;
import com.example.oopPTIT.dto.request.UpdateAttendanceRequest;
import com.example.oopPTIT.dto.response.AttendanceResponse;
import com.example.oopPTIT.dto.response.AttendanceResultsResponse;
import com.example.oopPTIT.dto.response.AttendanceSessionResponse;
import com.example.oopPTIT.dto.response.ClassSessionResponse;
import com.example.oopPTIT.dto.response.ResponseData;
import com.example.oopPTIT.dto.response.ResponsePage;
import com.example.oopPTIT.service.AttendanceService;
import com.example.oopPTIT.service.ClassSessionService;
import com.example.oopPTIT.service.CourseClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
public class LecturerCourseClassController {
    private final CourseClassService courseClassService;
    private final AttendanceService attendanceService;
    private final ClassSessionService classSessionService;

    @GetMapping("/lecturer/classes")
    public ResponseData<ResponsePage<?>> classes(@PageableDefault(size = 20, sort = "id") Pageable pageable,
            @RequestParam(name = "courseClass", required = false) String[] filters) {
        return new ResponseData<>(200, "Assigned classes retrieved successfully",
                courseClassService.getAssignedCourseClasses(pageable, filters));
    }

    @GetMapping("/classes/{id}/sessions")
    public ResponseData<ResponsePage<List<ClassSessionResponse>>> sessions(@PathVariable("id") Long id,
            @ParameterObject @PageableDefault(size = 20, sort = "sessionNo") Pageable pageable) {
        return new ResponseData<>(200, "Class sessions retrieved successfully",
                classSessionService.getSessions(id, pageable));
    }

    @PostMapping("/sessions/{id}/attendance/open")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<ResponseData<AttendanceSessionResponse>> openAttendanceSession(@PathVariable("id") Long id,
            @Valid @RequestBody OpenAttendanceSessionRequest request) {
        return ResponseEntity.status(201).body(new ResponseData<>(201, "Attendance session opened successfully",
                attendanceService.openAttendanceSession(id, request)));
    }

    @GetMapping("/sessions/{id}/attendance")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseData<AttendanceSessionResponse> getAttendanceSession(@PathVariable("id") Long id) {
        return new ResponseData<>(200, "Attendance session retrieved successfully",
                attendanceService.getAttendanceSession(id));
    }

    @PostMapping("/sessions/{id}/attendance/manual")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<ResponseData<AttendanceResponse>> manualAttendance(@PathVariable("id") Long id,
            @Valid @RequestBody ManualAttendanceRequest request) {
        return ResponseEntity.status(201).body(new ResponseData<>(201, "Attendance recorded successfully",
                attendanceService.recordManualAttendance(id, request)));
    }

    @PatchMapping("/attendance/{id}")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseData<AttendanceResponse> updateAttendance(@PathVariable("id") Long id,
            @Valid @RequestBody UpdateAttendanceRequest request) {
        return new ResponseData<>(200, "Attendance updated successfully",
                attendanceService.updateAttendance(id, request));
    }

    @PostMapping("/sessions/{id}/attendance/close")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseData<AttendanceSessionResponse> closeAttendanceSession(@PathVariable("id") Long id) {
        return new ResponseData<>(200, "Attendance session closed successfully",
                attendanceService.closeAttendanceSession(id));
    }

    @GetMapping("/sessions/{id}/attendance/results")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseData<AttendanceResultsResponse> attendanceResults(@PathVariable("id") Long id,
            @ParameterObject @PageableDefault(size = 20, sort = "student.studentCode") Pageable pageable) {
        return new ResponseData<>(200, "Attendance results retrieved successfully",
                attendanceService.getAttendanceResults(id, pageable));
    }

    @PostMapping("/lecturer/classes/{classId}/attendance")
    public ResponseEntity<ResponseData<AttendanceResponse>> attendance(@PathVariable Long classId,
            @Valid @RequestBody RecordAttendanceRequest request) {
        return ResponseEntity.status(201).body(new ResponseData<>(201, "Attendance recorded successfully",
                attendanceService.recordAttendance(classId, request)));
    }
}
