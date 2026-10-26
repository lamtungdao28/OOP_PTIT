package com.example.oopPTIT.repository;

import com.example.oopPTIT.entity.Attendance;
import com.example.oopPTIT.util.TimeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    boolean existsByAttendanceSession_IdAndStudent_Id(Long sessionId, Long studentId);

    @Query("select a.attendanceSession.classSession.courseClass.id from Attendance a where a.id = :attendanceId")
    Optional<Long> findCourseClassIdById(Long attendanceId);

    @EntityGraph(attributePaths = {"student", "attendanceSession.classSession.courseClass"})
    Page<Attendance> findByStudent_User_Id(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "student")
    Page<Attendance> findByAttendanceSession_Id(Long sessionId, Pageable pageable);

    @Query("select a.status as status, count(a) as total from Attendance a "
            + "where a.attendanceSession.id = :sessionId group by a.status")
    List<StatusCount> countByStatusForSession(Long sessionId);

    interface StatusCount {
        TimeStatus getStatus();
        long getTotal();
    }
}
