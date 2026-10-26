package com.example.oopPTIT.repository;

import com.example.oopPTIT.entity.StudentHasCourseClass;
import com.example.oopPTIT.util.Status;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentHasCourseClassRepository
        extends JpaRepository<StudentHasCourseClass, Long> {

    @EntityGraph(
            attributePaths = {
                    "student",
                    "student.user"
            }
    )
    List<StudentHasCourseClass>
    findByCourseClassIdAndStatus(
            Long courseClassId,
            Status status
    );
}