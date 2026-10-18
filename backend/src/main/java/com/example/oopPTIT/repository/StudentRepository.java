package com.example.oopPTIT.repository;

import com.example.oopPTIT.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByStudentCodeIgnoreCaseAndIdNot(String studentCode, Long id);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    boolean existsByStudentCodeIgnoreCase(String studentCode);

    boolean existsByPhone(String phone);

    List<Student> findByFullNameContainingIgnoreCaseAndStudentCodeContainingIgnoreCaseOrderByIdAsc(
            String fullName, String studentCode);
}

//mô tả bài toán -> ERD -> chuẩn hóa -> implement -> bảng đầy đủ
