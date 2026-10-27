package com.example.oopPTIT.service;

import com.example.oopPTIT.repository.AttendanceSessionRepository;
import com.example.oopPTIT.repository.ClassSessionRepository;
import com.example.oopPTIT.repository.FaceProfileRepository;
import com.example.oopPTIT.security.CourseAccess;
import com.example.oopPTIT.util.SessionStatus;
import com.example.oopPTIT.util.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FaceAttendanceReadService {
    private final ClassSessionRepository classSessionRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final FaceProfileRepository faceProfileRepository;
    private final CourseAccess courseAccess;

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('LECTURER')")
    public List<FaceAiClient.Candidate> candidates(Long classSessionId) {
        var classSession = classSessionRepository.findById(classSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class session not found"));
        courseAccess.requireAssignedLecturer(classSession.getCourseClass());
        var session = attendanceSessionRepository.findByClassSession_Id(classSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance session has not been opened"));
        if (session.getStatus() != SessionStatus.OPEN || !LocalDateTime.now().isBefore(session.getClosedAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Attendance session is closed");
        }
        List<FaceAiClient.Candidate> candidates = faceProfileRepository
                .findActiveCandidatesForClass(classSession.getCourseClass().getId(), Status.ACTIVE).stream()
                .filter(profile -> FaceEmbeddingCodec.MODEL_NAME.equals(profile.getModelName()))
                .map(profile -> new FaceAiClient.Candidate(profile.getStudent().getId(), profile.getModelName(),
                        FaceEmbeddingCodec.decode(profile.getEmbedding())))
                .toList();
        if (candidates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No enrolled student has an active face profile");
        }
        if (candidates.size() > 500) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Class has more than 500 face candidates");
        }
        return candidates;
    }
}
