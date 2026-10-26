package com.example.oopPTIT.controller;

import com.example.oopPTIT.dto.request.*;
import com.example.oopPTIT.dto.response.ResponseData;
import com.example.oopPTIT.service.*;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api")
public class FaceAttendanceController {

    private final FaceProfileService profileService;
    private final FaceRecognitionService recognitionService;
    private final AttendanceService attendanceService;

    private final String demoKey;

    public FaceAttendanceController(
            FaceProfileService profileService,
            FaceRecognitionService recognitionService,
            AttendanceService attendanceService,
            @Value("${FACE_DEMO_API_KEY:}") String demoKey
    ) {

        this.profileService = profileService;
        this.recognitionService = recognitionService;
        this.attendanceService = attendanceService;
        this.demoKey = demoKey;
    }

    // ==============================
    // 1. Đăng ký khuôn mặt
    // ==============================

    @PostMapping(
            value = "/admin/students/{studentId}/face-profile",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseData<FaceProfileService.FaceProfileResult>
    registerFace(
            @PathVariable Long studentId,
            @RequestHeader(
                    value = "X-Demo-Key",
                    required = false
            ) String key,
            @Valid @ModelAttribute FaceProfileRequest request
    ) {

        checkDemoKey(key);

        return new ResponseData<>(
                200,
                "Face registered successfully",
                profileService.register(
                        studentId,
                        request.getImage()
                )
        );
    }

    // ==============================
    // 2. Mở phiên điểm danh
    // ==============================

    @PostMapping(
            "/sessions/{classSessionId}/attendance/open"
    )
    public ResponseData<AttendanceService.SessionResult>
    openAttendance(
            @PathVariable Long classSessionId,
            @RequestHeader(
                    value = "X-Demo-Key",
                    required = false
            ) String key,
            @Valid @RequestBody OpenAttendanceSessionRequest request
    ) {

        checkDemoKey(key);

        return new ResponseData<>(
                200,
                "Attendance opened",
                attendanceService.open(
                        classSessionId,
                        request.getLateAfterMinutes()
                )
        );
    }

    // ==============================
    // 3. Nhận diện và điểm danh
    // ==============================

    @PostMapping(
            value = "/sessions/{classSessionId}/attendance/recognize",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseData<FaceRecognitionService.RecognitionResult>
    recognize(
            @PathVariable Long classSessionId,
            @RequestHeader(
                    value = "X-Demo-Key",
                    required = false
            ) String key,
            @Valid @ModelAttribute RecognizeAttendanceRequest request
    ) {

        checkDemoKey(key);

        return new ResponseData<>(
                200,
                "Recognition completed",
                recognitionService.recognize(
                        classSessionId,
                        request.getImage()
                )
        );
    }

    // ==============================
    // 4. Điểm danh thủ công
    // ==============================

    @PostMapping(
            "/sessions/{classSessionId}/attendance/manual"
    )
    public ResponseData<AttendanceService.MarkResult>
    manual(
            @PathVariable Long classSessionId,
            @RequestHeader(
                    value = "X-Demo-Key",
                    required = false
            ) String key,
            @Valid @RequestBody ManualAttendanceRequest request
    ) {

        checkDemoKey(key);

        return new ResponseData<>(
                200,
                "Manual attendance saved",
                attendanceService.markManual(
                        classSessionId,
                        request.getStudentId(),
                        request.getStatus()
                )
        );
    }

    // ==============================
    // 5. Đóng phiên điểm danh
    // ==============================

    @PostMapping(
            "/sessions/{classSessionId}/attendance/close"
    )
    public ResponseData<AttendanceService.SessionResult>
    closeAttendance(
            @PathVariable Long classSessionId,
            @RequestHeader(
                    value = "X-Demo-Key",
                    required = false
            ) String key
    ) {

        checkDemoKey(key);

        return new ResponseData<>(
                200,
                "Attendance closed",
                attendanceService.close(classSessionId)
        );
    }

    // ==============================
    // Demo access guard
    // ==============================

    private void checkDemoKey(String provided) {

        if (demoKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "FACE_DEMO_API_KEY not configured"
            );
        }

        if (provided == null
                || !MessageDigest.isEqual(
                demoKey.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8)
        )) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Invalid demo API key"
            );
        }
    }
}