package com.example.oopPTIT.repository;

import com.example.oopPTIT.entity.ClassSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {
    Page<ClassSession> findByCourseClass_Id(Long classId, Pageable pageable);
}
