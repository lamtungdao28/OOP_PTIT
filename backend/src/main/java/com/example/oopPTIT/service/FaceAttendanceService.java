package com.example.oopPTIT.service;

import com.example.oopPTIT.dto.response.AttendanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class FaceAttendanceService {
    private final FaceAttendanceReadService readService;
    private final FaceAiClient aiClient;
    private final AttendanceService attendanceService;

    @PreAuthorize("hasRole('LECTURER')")
    public AttendanceResponse recognize(Long classSessionId, MultipartFile image) {
        FaceImageValidator.validate(image);
        var candidates = readService.candidates(classSessionId);
        var result = aiClient.recognize(image, candidates);
        if (result == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI service returned no result");
        }
        if (result.studentId() == null) {
            throw new ResponseStatusException(HttpStatus.valueOf(422), "No unambiguous face match");
        }
        if (!FaceEmbeddingCodec.MODEL_NAME.equals(result.modelName()) || result.confidence() == null
                || !Double.isFinite(result.confidence()) || result.confidence() < 0.363
                || result.confidence() > 1.0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI service returned an invalid match");
        }
        var matched = candidates.stream()
                .filter(candidate -> candidate.studentId().equals(result.studentId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "AI service returned a student outside the candidate list"));
        BigDecimal confidence = BigDecimal.valueOf(result.confidence()).setScale(4, RoundingMode.HALF_UP);
        return attendanceService.recordFaceAttendance(classSessionId, result.studentId(), confidence,
                result.modelName(), FaceEmbeddingCodec.encode(matched.embedding()));
    }
}
