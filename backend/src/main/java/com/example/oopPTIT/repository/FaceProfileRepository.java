package com.example.oopPTIT.repository;

import com.example.oopPTIT.entity.FaceProfile;
import com.example.oopPTIT.util.Status;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FaceProfileRepository
        extends JpaRepository<FaceProfile, Long> {

    @EntityGraph(attributePaths = "student")
    Optional<FaceProfile> findByStudentId(
            Long studentId
    );

    @EntityGraph(attributePaths = "student")
    List<FaceProfile>
    findByStudentIdInAndStatusAndModelName(
            Collection<Long> studentIds,
            Status status,
            String modelName
    );
}