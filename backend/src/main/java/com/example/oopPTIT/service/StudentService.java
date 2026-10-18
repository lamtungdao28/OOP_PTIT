package com.example.oopPTIT.service;

import com.example.oopPTIT.dto.request.CreateStudentRequest;
import com.example.oopPTIT.dto.request.UpdateStudentRequest;
import com.example.oopPTIT.dto.request.UpdateStudentStatusRequest;
import com.example.oopPTIT.dto.response.StudentDetailResponse;
import com.example.oopPTIT.exception.ResourceNotFoundException;
import com.example.oopPTIT.entity.Student;
import com.example.oopPTIT.entity.User;
import com.example.oopPTIT.exception.DuplicateStudentException;
import com.example.oopPTIT.repository.UserRepository;
import com.example.oopPTIT.util.Role;
import com.example.oopPTIT.util.Status;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import com.example.oopPTIT.dto.response.StudentResponse;
import com.example.oopPTIT.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request) {
        String username = request.getUsername().strip();
        String email = request.getEmail().strip();
        String studentCode = request.getStudentCode().strip();
        String phone = request.getPhone().strip();

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateStudentException("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateStudentException("Email already exists");
        }
        if (studentRepository.existsByStudentCodeIgnoreCase(studentCode)) {
            throw new DuplicateStudentException("Student code already exists");
        }
        if (studentRepository.existsByPhone(phone)) {
            throw new DuplicateStudentException("Phone already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.STUDENT);
        user.setStatus(Status.ACTIVE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user = userRepository.save(user);

        Student student = new Student();
        student.setStudentCode(studentCode);
        student.setFullName(request.getFullName().strip());
        student.setDateOfBirth(request.getDateOfBirth());
        student.setPhone(phone);
        student.setUser(user);
        student.setCreatedAt(now);
        student.setUpdatedAt(now);
        student = studentRepository.saveAndFlush(student);

        return new StudentResponse(student.getId(), student.getStudentCode(), student.getFullName(),
                student.getDateOfBirth(), student.getPhone());
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> searchStudents(String fullName, String studentCode) {
        return studentRepository
                .findByFullNameContainingIgnoreCaseAndStudentCodeContainingIgnoreCaseOrderByIdAsc(
                        normalize(fullName), normalize(studentCode))
                .stream()
                .map(student -> new StudentResponse(
                        student.getId(),
                        student.getStudentCode(),
                        student.getFullName(),
                        student.getDateOfBirth(),
                        student.getPhone()))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentDetailResponse getStudent(Long id) {
        return StudentDetailResponse.from(findStudent(id));
    }

    @Transactional
    public StudentDetailResponse updateStudent(Long id, UpdateStudentRequest request) {
        Student student = findStudent(id);
        User user = student.getUser();
        if (request.getStudentCode() != null) {
            String code = request.getStudentCode().strip();
            if (studentRepository.existsByStudentCodeIgnoreCaseAndIdNot(code, id)) {
                throw new DuplicateStudentException("Student code already exists");
            }
            student.setStudentCode(code);
        }
        if (request.getPhone() != null) {
            String phone = request.getPhone().strip();
            if (studentRepository.existsByPhoneAndIdNot(phone, id)) {
                throw new DuplicateStudentException("Phone already exists");
            }
            student.setPhone(phone);
        }
        if (request.getEmail() != null) {
            String email = request.getEmail().strip();
            if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId())) {
                throw new DuplicateStudentException("Email already exists");
            }
            user.setEmail(email);
            user.setUpdatedAt(LocalDateTime.now());
        }
        if (request.getFullName() != null) student.setFullName(request.getFullName().strip());
        if (request.getDateOfBirth() != null) student.setDateOfBirth(request.getDateOfBirth());
        student.setUpdatedAt(LocalDateTime.now());
        studentRepository.flush();
        return StudentDetailResponse.from(student);
    }

    @Transactional
    public StudentDetailResponse updateStudentStatus(Long id, UpdateStudentStatusRequest request) {
        Student student = findStudent(id);
        student.getUser().setStatus(request.getStatus());
        student.getUser().setUpdatedAt(LocalDateTime.now());
        userRepository.flush();
        return StudentDetailResponse.from(student);
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id));
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
