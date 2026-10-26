package com.example.oopPTIT.service;

import com.example.oopPTIT.client.FaceAIClient;
import com.example.oopPTIT.entity.*;
import com.example.oopPTIT.repository.*;
import com.example.oopPTIT.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class FaceRecognitionService {

    private final FaceAIClient faceAIClient;
    private final FaceProfileRepository profileRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final StudentHasCourseClassRepository enrollmentRepository;
    private final AttendanceService attendanceService;

    private final double threshold;
    private final double minMargin;

    public FaceRecognitionService(
            FaceAIClient faceAIClient,
            FaceProfileRepository profileRepository,
            AttendanceSessionRepository sessionRepository,
            StudentHasCourseClassRepository enrollmentRepository,
            AttendanceService attendanceService,
            @Value("${FACE_MIN_SIMILARITY:0.60}")
            double threshold,
            @Value("${FACE_MIN_MARGIN:0.08}")
            double minMargin
    ) {

        this.faceAIClient = faceAIClient;
        this.profileRepository = profileRepository;
        this.sessionRepository = sessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.attendanceService = attendanceService;

        this.threshold = threshold;
        this.minMargin = minMargin;
    }

    public RecognitionResult recognize(
            Long classSessionId,
            MultipartFile image
    ) {

        // 1. Kiểm tra phiên điểm danh
        AttendanceSession session = sessionRepository
                .findByClassSessionId(classSessionId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Attendance session not opened"
                        )
                );

        if (session.getStatus() != SessionStatus.OPEN) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Attendance session is closed"
            );
        }

        // 2. Lấy danh sách sinh viên trong lớp
        Long courseClassId = session
                .getClassSession()
                .getCourseClass()
                .getId();

        List<Long> studentIds = enrollmentRepository
                .findByCourseClassIdAndStatus(
                        courseClassId,
                        Status.ACTIVE
                )
                .stream()
                .filter(e ->
                        e.getStudent().getUser().getStatus()
                                == Status.ACTIVE
                )
                .map(e -> e.getStudent().getId())
                .distinct()
                .toList();

        if (studentIds.isEmpty()) {
            return RecognitionResult.unknown(
                    "NO_ACTIVE_STUDENTS"
            );
        }

        // 3. Gọi Python lấy embedding
        FaceAIClient.EmbeddingResponse response =
                faceAIClient.extract(image);

        float[] newEmbedding =
                FaceEmbeddingUtil.fromBytes(
                        FaceEmbeddingUtil.toBytes(
                                response.embedding()
                        )
                );

        // 4. Lấy các face profile cùng model
        List<FaceProfile> profiles =
                profileRepository
                        .findByStudentIdInAndStatusAndModelName(
                                studentIds,
                                Status.ACTIVE,
                                response.modelName()
                        );

        if (profiles.isEmpty()) {
            return RecognitionResult.unknown(
                    "NO_FACE_PROFILES"
            );
        }

        // 5. Tính similarity cho từng sinh viên
        List<Match> matches = new ArrayList<>();

        for (FaceProfile profile : profiles) {

            try {

                float[] storedEmbedding =
                        FaceEmbeddingUtil.fromBytes(
                                profile.getEmbedding()
                        );

                double similarity =
                        FaceEmbeddingUtil.cosineSimilarity(
                                newEmbedding,
                                storedEmbedding
                        );

                if (Double.isFinite(similarity)) {
                    matches.add(
                            new Match(profile, similarity)
                    );
                }

            } catch (IllegalArgumentException ex) {
                // Bỏ qua embedding lỗi hoặc không đúng số chiều
            }
        }

        matches.sort(
                Comparator.comparingDouble(
                        Match::similarity
                ).reversed()
        );

        if (matches.isEmpty()) {
            return RecognitionResult.unknown(
                    "NO_VALID_PROFILES"
            );
        }

        Match best = matches.get(0);

        // 6. Kiểm tra ngưỡng
        if (best.similarity() < threshold) {
            return RecognitionResult.unknown(
                    "UNKNOWN_FACE"
            );
        }

        // 7. So sánh với sinh viên đứng thứ hai
        if (matches.size() > 1) {

            double secondScore =
                    matches.get(1).similarity();

            if (best.similarity() - secondScore
                    < minMargin) {

                return RecognitionResult.unknown(
                        "AMBIGUOUS_FACE"
                );
            }
        }

        // 8. Ghi điểm danh
        Student student =
                best.profile().getStudent();

        AttendanceService.MarkResult result =
                attendanceService.recordAutomatic(
                        classSessionId,
                        student.getId(),
                        best.similarity()
                );

        return new RecognitionResult(
                true,
                "MATCHED",
                student.getId(),
                student.getStudentCode(),
                student.getFullName(),
                result.status(),
                best.similarity(),
                result.alreadyMarked()
        );
    }

    private record Match(
            FaceProfile profile,
            double similarity
    ) {}

    public record RecognitionResult(
            boolean recognized,
            String reason,
            Long studentId,
            String studentCode,
            String fullName,
            TimeStatus attendanceStatus,
            Double similarity,
            boolean alreadyMarked
    ) {

        public static RecognitionResult unknown(
                String reason
        ) {
            return new RecognitionResult(
                    false,
                    reason,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false
            );
        }
    }
}