package com.example.oopPTIT.repository;

import com.example.oopPTIT.entity.ClassSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {
    Page<ClassSession> findByCourseClass_Id(Long classId, Pageable pageable);

    boolean existsByCourseClass_IdAndSessionDateAndStartTimeAndEndTime(
            Long classId, LocalDate sessionDate, LocalTime startTime, LocalTime endTime);

    @Query("select coalesce(max(session.sessionNo), 0) from ClassSession session "
            + "where session.courseClass.id = :classId")
    Integer findMaxSessionNoByCourseClassId(Long classId);
}
