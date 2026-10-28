package com.example.oopPTIT.service;

import org.springframework.security.access.prepost.PreAuthorize;

import com.example.oopPTIT.dto.request.CreateStudentRequest;
import com.example.oopPTIT.dto.request.UpdateStudentRequest;
import com.example.oopPTIT.dto.request.UpdateStudentStatusRequest;
import com.example.oopPTIT.dto.response.StudentDetailResponse;
import com.example.oopPTIT.dto.response.ResponsePage;
import com.example.oopPTIT.exception.InvalidStudentFilterException;
import com.example.oopPTIT.exception.ResourceNotFoundException;
import com.example.oopPTIT.entity.Student;
import com.example.oopPTIT.entity.User;
import com.example.oopPTIT.exception.DuplicateStudentException;
import com.example.oopPTIT.repository.UserRepository;
import com.example.oopPTIT.repository.FaceProfileRepository;
import com.example.oopPTIT.repository.specification.StudentSpecificationBuilder;
import com.example.oopPTIT.util.Role;
import com.example.oopPTIT.util.Status;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import com.example.oopPTIT.dto.response.StudentResponse;
import com.example.oopPTIT.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class StudentService {

    private static final Set<String> SORT_FIELDS = Set.of("id", "studentCode", "fullName", "dateOfBirth", "phone");

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FaceProfileRepository faceProfileRepository;

    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request) {
        String username = request.getUsername().strip();
        String email = request.getEmail().strip();
        String studentCode = request.getStudentCode().strip();
        String phone = request.getPhone().strip();

        if (userRepository.existsByUsernameIgnoreCase(username) || userRepository.existsByEmailIgnoreCase(username)) {
            throw new DuplicateStudentException("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(email) || userRepository.existsByUsernameIgnoreCase(email)) {
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
    public ResponsePage<List<StudentResponse>> advanceSearchWithSpecification(Pageable pageable, String[] student) {
        return advanceSearchWithSpecification(pageable, student, null);
    }

    @Transactional(readOnly = true)
    public ResponsePage<List<StudentResponse>> advanceSearchWithSpecification(
            Pageable pageable, String[] student, String search) {
        pageable.getSort().forEach(order -> {
            if (!SORT_FIELDS.contains(order.getProperty())) {
                throw new InvalidStudentFilterException("Unsupported student sort field: " + order.getProperty());
            }
        });

        StudentSpecificationBuilder builder = new StudentSpecificationBuilder();
        if (student != null) {
            for (String filter : student) {
                builder.with(filter);
            }
        }

        var specification = builder.build();
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.strip().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("studentCode")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), pattern)));
        }

        Page<StudentResponse> students = studentRepository.findAll(specification, pageable)
                .map(item -> new StudentResponse(item.getId(), item.getStudentCode(), item.getFullName(),
                        item.getDateOfBirth(), item.getPhone()));

        return ResponsePage.<List<StudentResponse>>builder()
                .pageNo(students.getNumber())
                .pageSize(students.getSize())
                .totalPage(students.getTotalPages())
                .items(students.getContent())
                .build();
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
            if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId())
                    || userRepository.existsByUsernameIgnoreCaseAndIdNot(email, user.getId())) {
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

    @Transactional
    public void deleteStudent(Long id) {
        Student student = findStudent(id);
        if (!student.getStudentHasCourseClasses().isEmpty() || !student.getAttendances().isEmpty()
                || faceProfileRepository.existsByStudent_Id(id)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Không thể xóa sinh viên đã có lớp học, điểm danh hoặc hồ sơ khuôn mặt");
        }
        User user = student.getUser();
        studentRepository.delete(student);
        studentRepository.flush();
        userRepository.delete(user);
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id));
    }

}
