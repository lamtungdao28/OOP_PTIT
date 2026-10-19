package com.example.oopPTIT.repository;

import com.example.oopPTIT.entity.Lecturer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LecturerRepository extends JpaRepository<Lecturer, Long>, JpaSpecificationExecutor<Lecturer> {
    boolean existsByLecturerCodeIgnoreCase(String lecturerCode);
    boolean existsByLecturerCodeIgnoreCaseAndIdNot(String lecturerCode, Long id);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<Lecturer> findAll(Specification<Lecturer> specification, Pageable pageable);
}
