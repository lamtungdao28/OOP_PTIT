package com.example.oopPTIT.repository;

import com.example.oopPTIT.entity.AttendanceSession;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AttendanceSessionRepository
        extends JpaRepository<AttendanceSession, Long> {

    boolean existsByClassSessionId(
            Long classSessionId
    );

    @EntityGraph(
            attributePaths = {
                    "classSession",
                    "classSession.courseClass"
            }
    )
    Optional<AttendanceSession> findByClassSessionId(
            Long classSessionId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM AttendanceSession s
        JOIN FETCH s.classSession c
        WHERE c.id = :id
    """)
    Optional<AttendanceSession> lockByClassSessionId(
            @Param("id") Long classSessionId
    );
}