package com.example.oopPTIT.repository;

import com.example.oopPTIT.entity.FaceProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import com.example.oopPTIT.util.Status;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface FaceProfileRepository extends JpaRepository<FaceProfile, Long> {
    Optional<FaceProfile> findByStudent_Id(Long studentId);
    boolean existsByStudent_Id(Long studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FaceProfile f where f.student.id = :studentId")
    Optional<FaceProfile> findByStudentIdForUpdate(@Param("studentId") Long studentId);

    @Query("""
            select distinct f from FaceProfile f
            join f.student s join s.studentHasCourseClasses e
            where e.courseClass.id = :classId and e.status = :active
              and f.status = :active and s.user.status = :active
            """)
    List<FaceProfile> findActiveCandidatesForClass(@Param("classId") Long classId,
                                                    @Param("active") Status active);
}
