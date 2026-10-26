package com.example.oopPTIT.service;

import com.example.oopPTIT.client.FaceAIClient;
import com.example.oopPTIT.entity.FaceProfile;
import com.example.oopPTIT.entity.Student;
import com.example.oopPTIT.repository.FaceProfileRepository;
import com.example.oopPTIT.repository.StudentRepository;
import com.example.oopPTIT.util.FaceEmbeddingUtil;
import com.example.oopPTIT.util.Status;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FaceProfileService {

    private final FaceAIClient faceAIClient;
    private final FaceProfileRepository faceProfileRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public FaceProfileResult register(
            Long studentId,
            MultipartFile image
    ) {

        Student student = studentRepository
                .findById(studentId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Student not found"
                        )
                );

        if (student.getUser().getStatus() != Status.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Student account is inactive"
            );
        }

        // Bước 1: Gọi Python lấy embedding
        FaceAIClient.EmbeddingResponse response =
                faceAIClient.extract(image);

        // Bước 2: Chuyển vector thành bytes
        byte[] embeddingBytes =
                FaceEmbeddingUtil.toBytes(
                        response.embedding()
                );

        // Bước 3: Tìm profile hiện tại
        FaceProfile profile = faceProfileRepository
                .findByStudentId(studentId)
                .orElseGet(FaceProfile::new);

        // Bước 4: Lưu dữ liệu khuôn mặt
        profile.setStudent(student);
        profile.setEmbedding(embeddingBytes);
        profile.setModelName(response.modelName());
        profile.setStatus(Status.ACTIVE);

        faceProfileRepository.save(profile);

        return new FaceProfileResult(
                studentId,
                response.modelName(),
                Status.ACTIVE
        );
    }

    public record FaceProfileResult(
            Long studentId,
            String modelName,
            Status status
    ) {}
}