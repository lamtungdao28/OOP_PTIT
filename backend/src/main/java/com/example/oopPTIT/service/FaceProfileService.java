package com.example.oopPTIT.service;

import com.example.oopPTIT.dto.response.FaceProfileResponse;
import com.example.oopPTIT.entity.FaceProfile;
import com.example.oopPTIT.entity.Student;
import com.example.oopPTIT.repository.FaceProfileRepository;
import com.example.oopPTIT.repository.StudentRepository;
import com.example.oopPTIT.security.CurrentUser;
import com.example.oopPTIT.util.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;


@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class FaceProfileService {
    private final FaceProfileRepository faceProfileRepository;
    private final StudentRepository studentRepository;
    private final CurrentUser currentUser;
    private final FaceAiClient faceAiClient;

    @Transactional(readOnly = true)
    public FaceProfileResponse getMyProfile() {
        Student student = currentStudent();
        return faceProfileRepository.findByStudent_Id(student.getId())
                .map(FaceProfileResponse::from).orElseGet(FaceProfileResponse::empty);
    }

    @Transactional
    public FaceProfileResponse register(MultipartFile image) {
        Student student = currentStudent();
        if (faceProfileRepository.existsByStudent_Id(student.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Face profile already exists; use PUT to replace it");
        }
        FaceImageValidator.validate(image);
        FaceAiClient.RegistrationResult result = faceAiClient.register(image);
        FaceProfile profile = new FaceProfile();
        profile.setStudent(student);
        applyAiResult(profile, result);
        try {
            return FaceProfileResponse.from(faceProfileRepository.saveAndFlush(profile));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Face profile already exists", exception);
        }
    }

    @Transactional
    public FaceProfileResponse replace(MultipartFile image) {
        Student student = currentStudent();
        FaceProfile profile = faceProfileRepository.findByStudent_Id(student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Face profile not found; use POST first"));
        FaceImageValidator.validate(image);
        FaceAiClient.RegistrationResult result = faceAiClient.register(image);
        applyAiResult(profile, result);
        return FaceProfileResponse.from(faceProfileRepository.saveAndFlush(profile));
    }

    private Student currentStudent() {
        return studentRepository.findByUser_Id(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found"));
    }

    private void applyAiResult(FaceProfile profile, FaceAiClient.RegistrationResult result) {
        if (result.modelName() == null || result.modelName().isBlank() || result.modelName().length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI service returned an invalid model name");
        }
        profile.setModelName(result.modelName());
        profile.setEmbedding(FaceEmbeddingCodec.encode(result.embedding()));
        profile.setStatus(Status.ACTIVE);
    }
}
