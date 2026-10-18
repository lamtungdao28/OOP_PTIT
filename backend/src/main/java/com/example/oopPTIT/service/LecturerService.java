package com.example.oopPTIT.service;

import com.example.oopPTIT.dto.request.CreateLecturerRequest;
import com.example.oopPTIT.dto.request.UpdateLecturerRequest;
import com.example.oopPTIT.dto.response.LecturerResponse;
import com.example.oopPTIT.entity.Lecturer;
import com.example.oopPTIT.entity.User;
import com.example.oopPTIT.exception.DuplicateLecturerException;
import com.example.oopPTIT.exception.ResourceNotFoundException;
import com.example.oopPTIT.repository.LecturerRepository;
import com.example.oopPTIT.repository.UserRepository;
import com.example.oopPTIT.util.Role;
import com.example.oopPTIT.util.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LecturerService {
    private final LecturerRepository lecturerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<LecturerResponse> getLecturers() {
        return lecturerRepository.findAllByOrderByIdAsc().stream().map(LecturerResponse::from).toList();
    }

    @Transactional
    public LecturerResponse createLecturer(CreateLecturerRequest request) {
        String username = request.getUsername().strip();
        String email = request.getEmail().strip();
        String code = request.getLecturerCode().strip();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateLecturerException("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
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
        Lecturer lecturer = lecturerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lecturer not found: " + id));
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
            if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId())) {
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
}
