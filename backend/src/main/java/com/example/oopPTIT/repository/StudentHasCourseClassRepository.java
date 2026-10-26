package com.example.oopPTIT.repository;

import com.example.oopPTIT.entity.StudentHasCourseClass;
import com.example.oopPTIT.entity.Student;
import com.example.oopPTIT.util.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentHasCourseClassRepository extends JpaRepository<StudentHasCourseClass, Long> {
    boolean existsByCourseClass_IdAndStudent_IdAndStatus(Long classId, Long studentId, Status status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from StudentHasCourseClass e where e.courseClass.id = :classId "
            + "and e.student.id = :studentId and e.status = :status")
    List<StudentHasCourseClass> findActiveForUpdate(Long classId, Long studentId, Status status);
    long deleteByCourseClass_IdAndStudent_Id(Long courseClassId, Long studentId);

    @Query("""
            select distinct e.student from StudentHasCourseClass e
            where e.courseClass.id = :classId and e.status = :status
              and not exists (select a.id from Attendance a
                              where a.attendanceSession.id = :sessionId and a.student.id = e.student.id)
            """)
    List<Student> findStudentsWithoutAttendance(Long classId, Long sessionId, Status status);

    @Query("""
            select count(distinct e.student.id) from StudentHasCourseClass e
            where e.courseClass.id = :classId and e.status = :status
              and not exists (select a.id from Attendance a
                              where a.attendanceSession.id = :sessionId and a.student.id = e.student.id)
            """)
    long countStudentsWithoutAttendance(Long classId, Long sessionId, Status status);
}
