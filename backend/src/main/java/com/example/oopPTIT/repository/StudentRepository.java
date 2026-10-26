package com.example.oopPTIT.repository;

import com.example.oopPTIT.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    Optional<Student> findByUser_Username(String username);
    Optional<Student> findByUser_Id(Long userId);

    boolean existsByStudentCodeIgnoreCaseAndIdNot(String studentCode, Long id);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    boolean existsByStudentCodeIgnoreCase(String studentCode);

    boolean existsByPhone(String phone);

}

//mô tả bài toán -> ERD -> chuẩn hóa -> implement -> bảng đầy đủ
