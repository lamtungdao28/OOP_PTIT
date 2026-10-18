package com.example.oopPTIT.repository;

import com.example.oopPTIT.entity.Lecturer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LecturerRepository extends JpaRepository<Lecturer, Long> {
    boolean existsByLecturerCodeIgnoreCase(String lecturerCode);
    boolean existsByLecturerCodeIgnoreCaseAndIdNot(String lecturerCode, Long id);

    @EntityGraph(attributePaths = "user")
    List<Lecturer> findAllByOrderByIdAsc();
}
