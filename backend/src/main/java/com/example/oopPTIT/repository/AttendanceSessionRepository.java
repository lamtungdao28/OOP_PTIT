package com.example.oopPTIT.repository;

import com.example.oopPTIT.entity.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    Optional<AttendanceSession> findByClassSession_Id(Long classSessionId);

    boolean existsByClassSession_Id(Long classSessionId);
}
