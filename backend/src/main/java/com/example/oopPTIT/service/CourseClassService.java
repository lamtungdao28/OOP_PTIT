package com.example.oopPTIT.service;

import com.example.oopPTIT.dto.request.AssignLecturerRequest;
import com.example.oopPTIT.dto.request.CreateCourseClassRequest;
import com.example.oopPTIT.dto.request.EnrollStudentRequest;
import com.example.oopPTIT.dto.request.UpdateCourseClassRequest;
import com.example.oopPTIT.dto.response.CourseClassResponse;
import com.example.oopPTIT.dto.response.ResponsePage;
import com.example.oopPTIT.dto.response.StudentHasCourseClassResponse;
import com.example.oopPTIT.dto.response.StudentResponse;
import com.example.oopPTIT.entity.*;
import com.example.oopPTIT.repository.*;
import com.example.oopPTIT.repository.specification.CourseClassForStudentSpecification;
import com.example.oopPTIT.repository.specification.CourseClassSpecificationBuilder;
import com.example.oopPTIT.repository.specification.StudentInCourseClassSpecification;
import com.example.oopPTIT.repository.specification.StudentSpecificationBuilder;
import com.example.oopPTIT.util.Role;
import com.example.oopPTIT.util.Status;
import com.example.oopPTIT.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CourseClassService {
    private static final Pattern SEARCH_PATTERN = Pattern.compile("(\\w+?)([<:>~!])(.*)(\\p{Punct}?)(\\p{Punct}?)");

    private final CourseClassRepository courseClassRepository;
    private final SubjectRepository subjectRepository;
    private final SemesterRepository semesterRepository;
    private final LecturerRepository lecturerRepository;
    private final StudentHasCourseClassRepository studentHasCourseClassRepository;
    private final StudentRepository studentRepository;
    private final CurrentUser currentUser;

    @Transactional
    public Long addCourseClass(CreateCourseClassRequest request) {
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Subject not found: " + request.getSubjectId()));
        Semester semester = semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Semester not found: " + request.getSemesterId()));
        Lecturer lecturer = lecturerRepository.findById(request.getLecturerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lecturer not found: " + request.getLecturerId()));

        CourseClass courseClass = new CourseClass();
        courseClass.setClassCode(request.getClassCode());
        courseClass.setMaxStudents(request.getMaxStudents());
        courseClass.setStatus(Status.ACTIVE);
        courseClass.setSubject(subject);
        courseClass.setSemester(semester);
        courseClass.setLecturer(lecturer);
        return courseClassRepository.save(courseClass).getId();
    }

    @Transactional(readOnly = true)
    public CourseClassResponse getCourseClassById(Long id) {
        CourseClass courseClass = courseClassRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Course class not found: " + id));
        return toResponse(courseClass);
    }

    @Transactional
    public CourseClassResponse assignLecturer(Long id, AssignLecturerRequest request) {
        CourseClass courseClass = courseClassRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Course class not found: " + id));
        Lecturer lecturer = lecturerRepository.findById(request.getLecturerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lecturer not found: " + request.getLecturerId()));

        courseClass.setLecturer(lecturer);
        return toResponse(courseClassRepository.saveAndFlush(courseClass));
    }

    @Transactional
    public CourseClassResponse updateCourseClass(Long id, UpdateCourseClassRequest request) {
        CourseClass courseClass = courseClassRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Course class not found: " + id));

        if (request.getClassCode() != null) {
            courseClass.setClassCode(request.getClassCode().strip());
        }
        if (request.getMaxStudents() != null) {
            courseClass.setMaxStudents(request.getMaxStudents());
        }
        if (request.getStatus() != null) {
            courseClass.setStatus(request.getStatus());
        }
        if (request.getSubjectId() != null) {
            Subject subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Subject not found: " + request.getSubjectId()));
            courseClass.setSubject(subject);
        }
        if (request.getSemesterId() != null) {
            Semester semester = semesterRepository.findById(request.getSemesterId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Semester not found: " + request.getSemesterId()));
            courseClass.setSemester(semester);
        }
        if (request.getLecturerId() != null) {
            Lecturer lecturer = lecturerRepository.findById(request.getLecturerId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Lecturer not found: " + request.getLecturerId()));
            courseClass.setLecturer(lecturer);
        }

        return toResponse(courseClassRepository.saveAndFlush(courseClass));
    }

    @Transactional(readOnly = true)
    public ResponsePage<?> findAllCourseClasses(Pageable pageable, String[] courseClass) {
        return findCourseClasses(pageable, courseClass, Specification.unrestricted());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('STUDENT') and #username == authentication.name")
    public ResponsePage<?> getEnrolledCourseClasses(String username, Pageable pageable, String[] courseClass) {
        Student student = studentRepository.findByUser_Username(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "An active student account is required"));
        User user = student.getUser();
        if (user.getRole() != Role.STUDENT || user.getStatus() != Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "An active student account is required");
        }
        return findCourseClasses(pageable, courseClass, new CourseClassForStudentSpecification(student.getId()));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    public ResponsePage<?> getAssignedCourseClasses(Pageable pageable, String[] filters) {
        Specification<CourseClass> specification = Specification.unrestricted();
        if (!currentUser.isAdmin()) {
            Long userId = currentUser.id();
            specification = (root, query, builder) -> builder.equal(root.get("lecturer").get("user").get("id"), userId);
        }
        return findCourseClasses(pageable, filters, specification);
    }

    private ResponsePage<?> findCourseClasses(Pageable pageable, String[] courseClass,
            Specification<CourseClass> specification) {
        validateSort(pageable, Set.of("id", "classCode", "maxStudents", "status", "createdAt", "updatedAt",
                "subject.id", "semester.id", "lecturer.id"));
        if (courseClass != null) {
            CourseClassSpecificationBuilder builder = new CourseClassSpecificationBuilder();
            for (String filter : courseClass) {
                Matcher matcher = SEARCH_PATTERN.matcher(filter);
                if (matcher.find()) {
                    builder.with(matcher.group(1), matcher.group(2), matcher.group(3),
                            matcher.group(4), matcher.group(5));
                }
            }
            Specification<CourseClass> filters = builder.build();
            if (filters != null) {
                specification = specification.and(filters);
            }
        }
        Page<CourseClassResponse> courseClasses = courseClassRepository.findAll(specification, pageable)
                .map(this::toResponse);
        return ResponsePage.builder()
                .pageNo(courseClasses.getNumber())
                .pageSize(courseClasses.getSize())
                .totalPage(courseClasses.getTotalPages())
                .items(courseClasses.getContent())
                .build();
    }

    private CourseClassResponse toResponse(CourseClass courseClass) {
        return CourseClassResponse.builder()
                .id(courseClass.getId())
                .classCode(courseClass.getClassCode())
                .maxStudents(courseClass.getMaxStudents())
                .status(courseClass.getStatus())
                .subjectId(courseClass.getSubject().getId())
                .semesterId(courseClass.getSemester().getId())
                .lecturerId(courseClass.getLecturer().getId())
                .createdAt(courseClass.getCreatedAt())
                .updatedAt(courseClass.getUpdatedAt())
                .build();
    }
    public Long addStudentToClass(Long id, EnrollStudentRequest request){
        CourseClass courseClass = courseClassRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Course class not found: " + id));
        Long studentId = request.getStudentId();
        Student student = studentRepository.findById(studentId).orElseThrow(()
        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found" + studentId ));

        StudentHasCourseClass studentHasCourseClass = StudentHasCourseClass.builder()
                .courseClass(courseClass)
                .student(student)
                .status(Status.ACTIVE)
                .build();
        studentHasCourseClassRepository.save(studentHasCourseClass);
        return studentHasCourseClass.getId();
    }

    @Transactional
    public void removeStudentFromClass(Long id, Long studentId) {
        if (!courseClassRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course class not found: " + id);
        }
        if (!studentRepository.existsById(studentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentId);
        }
        long deleted = studentHasCourseClassRepository.deleteByCourseClass_IdAndStudent_Id(id, studentId);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Student " + studentId + " is not enrolled in course class " + id);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER') and @courseAccess.canManage(#id)")
    @Transactional(readOnly = true)
    public ResponsePage<?> getStudentsInCourseClass(Long id, Pageable pageable, String[] studentFilters) {
        validateSort(pageable, Set.of("id", "studentCode", "fullName", "dateOfBirth", "phone", "createdAt", "updatedAt"));
        CourseClass courseClass = courseClassRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Course class not found: " + id));

        Specification<Student> specification = new StudentInCourseClassSpecification(id);
        if (studentFilters != null) {
            StudentSpecificationBuilder builder = new StudentSpecificationBuilder();
            for (String filter : studentFilters) {
                builder.with(filter);
            }
            specification = specification.and(builder.build());
        }
        CourseClassResponse courseClassResponse = toResponse(courseClass);
        Page<StudentHasCourseClassResponse> students = studentRepository.findAll(specification, pageable)
                .map(student -> StudentHasCourseClassResponse.builder()
                        .student(new StudentResponse(student.getId(), student.getStudentCode(),
                                student.getFullName(), student.getDateOfBirth(), student.getPhone()))
                        .courseClass(courseClassResponse)
                        .build());

        return ResponsePage.builder()
                .pageNo(students.getNumber())
                .pageSize(students.getSize())
                .totalPage(students.getTotalPages())
                .items(students.getContent())
                .build();
    }

    private void validateSort(Pageable pageable, Set<String> allowedFields) {
        if (pageable.getSort().stream().anyMatch(order -> !allowedFields.contains(order.getProperty()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field");
        }
    }
}
