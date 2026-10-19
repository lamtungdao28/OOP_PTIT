package com.example.oopPTIT.service;

import com.example.oopPTIT.dto.request.CreateCourseClassRequest;
import com.example.oopPTIT.dto.response.CourseClassResponse;
import com.example.oopPTIT.dto.response.ResponsePage;
import com.example.oopPTIT.entity.CourseClass;
import com.example.oopPTIT.entity.Lecturer;
import com.example.oopPTIT.entity.Semester;
import com.example.oopPTIT.entity.Subject;
import com.example.oopPTIT.repository.CourseClassRepository;
import com.example.oopPTIT.repository.LecturerRepository;
import com.example.oopPTIT.repository.SemesterRepository;
import com.example.oopPTIT.repository.SubjectRepository;
import com.example.oopPTIT.repository.specification.CourseClassSpecificationBuilder;
import com.example.oopPTIT.util.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CourseClassService {
    private static final Pattern SEARCH_PATTERN = Pattern.compile("(\\w+?)([<:>~!])(.*)(\\p{Punct}?)(\\p{Punct}?)");

    private final CourseClassRepository courseClassRepository;
    private final SubjectRepository subjectRepository;
    private final SemesterRepository semesterRepository;
    private final LecturerRepository lecturerRepository;

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

    @Transactional(readOnly = true)
    public ResponsePage<?> advanceSearchWithSpecification(Pageable pageable, String[] courseClass) {
        Specification<CourseClass> specification = Specification.unrestricted();
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
                specification = filters;
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
}
