package com.example.oopPTIT.service;

import org.springframework.security.access.prepost.PreAuthorize;

import com.example.oopPTIT.dto.request.CreateLecturerRequest;
import com.example.oopPTIT.dto.request.UpdateLecturerRequest;
import com.example.oopPTIT.dto.response.LecturerResponse;
import com.example.oopPTIT.dto.response.ResponsePage;
import com.example.oopPTIT.entity.Lecturer;
import com.example.oopPTIT.entity.User;
import com.example.oopPTIT.exception.DuplicateLecturerException;
import com.example.oopPTIT.exception.InvalidLecturerFilterException;
import com.example.oopPTIT.exception.ResourceNotFoundException;
import com.example.oopPTIT.repository.LecturerRepository;
import com.example.oopPTIT.repository.UserRepository;
import com.example.oopPTIT.repository.specification.LecturerSpecificationBuilder;
import com.example.oopPTIT.util.Role;
import com.example.oopPTIT.util.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class LecturerService {
    private static final Set<String> SORT_FIELDS = Set.of("id", "lecturerCode", "fullName", "department", "phone");

    private final LecturerRepository lecturerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ResponsePage<List<LecturerResponse>> advanceSearchWithSpecification(Pageable pageable, String[] lecturer) {
        return advanceSearchWithSpecification(pageable, lecturer, null);
    }

    public ResponsePage<List<LecturerResponse>> advanceSearchWithSpecification(
            Pageable pageable, String[] lecturer, String search) {
        pageable.getSort().forEach(order -> {
            if (!SORT_FIELDS.contains(order.getProperty())) {
                throw new InvalidLecturerFilterException("Unsupported lecturer sort field: " + order.getProperty());
            }
        });

        LecturerSpecificationBuilder builder = new LecturerSpecificationBuilder();
        if (lecturer != null) {
            for (String filter : lecturer) {
                builder.with(filter);
            }
        }

        var specification = builder.build();
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.strip().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("lecturerCode")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("department")), pattern)));
        }

        Page<LecturerResponse> lecturers = lecturerRepository.findAll(specification, pageable)
                .map(LecturerResponse::from);

        return ResponsePage.<List<LecturerResponse>>builder()
                .pageNo(lecturers.getNumber())
                .pageSize(lecturers.getSize())
                .totalPage(lecturers.getTotalPages())
                .items(lecturers.getContent())
                .build();
    }

    @Transactional(readOnly = true)
    public LecturerResponse getLecturer(Long id) {
        return LecturerResponse.from(findLecturer(id));
    }

    @Transactional
    public LecturerResponse createLecturer(CreateLecturerRequest request) {
        String username = request.getUsername().strip();
        String email = request.getEmail().strip();
        String code = request.getLecturerCode().strip();
        if (userRepository.existsByUsernameIgnoreCase(username) || userRepository.existsByEmailIgnoreCase(username)) {
            throw new DuplicateLecturerException("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(email) || userRepository.existsByUsernameIgnoreCase(email)) {
            throw new DuplicateLecturerException("Email already exists");
        }
        if (lecturerRepository.existsByLecturerCodeIgnoreCase(code)) {
            throw new DuplicateLecturerException("Lecturer code already exists");
        }
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.LECTURER);
        user.setStatus(Status.ACTIVE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user = userRepository.save(user);

        Lecturer lecturer = new Lecturer();
        lecturer.setLecturerCode(code);
        lecturer.setFullName(request.getFullName().strip());
        lecturer.setDepartment(request.getDepartment().strip());
        lecturer.setPhone(request.getPhone().strip());
        lecturer.setUser(user);
        lecturer.setCreatedAt(now);
        lecturer.setUpdatedAt(now);
        return LecturerResponse.from(lecturerRepository.saveAndFlush(lecturer));
    }

    @Transactional
    public LecturerResponse updateLecturer(Long id, UpdateLecturerRequest request) {
        Lecturer lecturer = findLecturer(id);
        User user = lecturer.getUser();
        if (request.getLecturerCode() != null) {
            String code = request.getLecturerCode().strip();
            if (lecturerRepository.existsByLecturerCodeIgnoreCaseAndIdNot(code, id)) {
                throw new DuplicateLecturerException("Lecturer code already exists");
            }
            lecturer.setLecturerCode(code);
        }
        if (request.getEmail() != null) {
            String email = request.getEmail().strip();
            if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId())
                    || userRepository.existsByUsernameIgnoreCaseAndIdNot(email, user.getId())) {
                throw new DuplicateLecturerException("Email already exists");
            }
            user.setEmail(email);
            user.setUpdatedAt(LocalDateTime.now());
        }
        if (request.getFullName() != null) lecturer.setFullName(request.getFullName().strip());
        if (request.getDepartment() != null) lecturer.setDepartment(request.getDepartment().strip());
        if (request.getPhone() != null) lecturer.setPhone(request.getPhone().strip());
        lecturer.setUpdatedAt(LocalDateTime.now());
        lecturerRepository.flush();
        return LecturerResponse.from(lecturer);
    }

    @Transactional
    public void deleteLecturer(Long id) {
        Lecturer lecturer = findLecturer(id);
        if (!lecturer.getCourseClasses().isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Không thể xóa giảng viên đang phụ trách lớp học phần");
        }
        User user = lecturer.getUser();
        lecturerRepository.delete(lecturer);
        lecturerRepository.flush();
        userRepository.delete(user);
    }

    private Lecturer findLecturer(Long id) {
        return lecturerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lecturer not found: " + id));
    }
}
