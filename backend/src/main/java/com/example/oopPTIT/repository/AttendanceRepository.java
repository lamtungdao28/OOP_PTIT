package com.example.oopPTIT.repository;

import com.example.oopPTIT.entity.Attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttendanceRepository
        extends JpaRepository<Attendance, Long> {

    Optional<Attendance>
    findByAttendanceSessionIdAndStudentId(
            Long attendanceSessionId,
            Long studentId
    );
}