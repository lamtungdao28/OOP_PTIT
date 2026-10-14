package com.example.oopPTIT.controller;

import com.example.oopPTIT.entity.CourseClass;
import com.example.oopPTIT.entity.Lecturer;
import com.example.oopPTIT.entity.Semester;
import com.example.oopPTIT.entity.Subject;
import com.example.oopPTIT.repository.CourseClassRepository;
import com.example.oopPTIT.repository.LecturerRepository;
import com.example.oopPTIT.repository.SemesterRepository;
import com.example.oopPTIT.repository.SubjectRepository;
import com.example.oopPTIT.service.CourseClassService;
import com.example.oopPTIT.util.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminCourseClassControllerTests {
    private static final String VALID_REQUEST = """
            {"classCode":"INT2204-01","subjectId":10,"semesterId":5,
             "lecturerId":51,"maxStudents":50}
            """;

    private CourseClassRepository courseClassRepository;
    private SubjectRepository subjectRepository;
    private SemesterRepository semesterRepository;
    private LecturerRepository lecturerRepository;
    private MockMvc mockMvc;
    private Subject subject;
    private Semester semester;
    private Lecturer lecturer;

    @BeforeEach
    void setUp() {
        courseClassRepository = mock(CourseClassRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        semesterRepository = mock(SemesterRepository.class);
        lecturerRepository = mock(LecturerRepository.class);
        var service = new CourseClassService(courseClassRepository, subjectRepository,
                semesterRepository, lecturerRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminCourseClassController(service)).build();

        subject = new Subject();
        subject.setId(10L);
        semester = new Semester();
        semester.setId(5L);
        lecturer = new Lecturer();
        lecturer.setId(51L);
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(subject));
        when(semesterRepository.findById(5L)).thenReturn(Optional.of(semester));
        when(lecturerRepository.findById(51L)).thenReturn(Optional.of(lecturer));
    }

    @Test
    void createsActiveClassWithAllReferencesAndReturnsId() throws Exception {
        when(courseClassRepository.save(any(CourseClass.class))).thenAnswer(invocation -> {
            CourseClass courseClass = invocation.getArgument(0);
            courseClass.setId(12L);
            return courseClass;
        });

        mockMvc.perform(post("/api/admin/classes").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("added course class"))
                .andExpect(jsonPath("$.data").value(12));

        var captor = ArgumentCaptor.forClass(CourseClass.class);
        verify(courseClassRepository).save(captor.capture());
        CourseClass saved = captor.getValue();
        assertThat(saved.getClassCode()).isEqualTo("INT2204-01");
        assertThat(saved.getMaxStudents()).isEqualTo(50);
        assertThat(saved.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(saved.getSubject()).isSameAs(subject);
        assertThat(saved.getSemester()).isSameAs(semester);
        assertThat(saved.getLecturer()).isSameAs(lecturer);
    }

    @Test
    void returns404WithoutSavingWhenSubjectDoesNotExist() throws Exception {
        when(subjectRepository.findById(10L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/classes").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isNotFound());

        verifyNoInteractions(courseClassRepository);
    }

    @Test
    void returns404WithoutSavingWhenSemesterDoesNotExist() throws Exception {
        when(semesterRepository.findById(5L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/classes").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isNotFound());

        verifyNoInteractions(courseClassRepository);
    }

    @Test
    void returns404WithoutSavingWhenLecturerDoesNotExist() throws Exception {
        when(lecturerRepository.findById(51L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/classes").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isNotFound());

        verifyNoInteractions(courseClassRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"classCode\":\" \",\"subjectId\":10,\"semesterId\":5,\"lecturerId\":51,\"maxStudents\":50}",
            "{\"classCode\":\"INT2204-01\",\"subjectId\":0,\"semesterId\":5,\"lecturerId\":51,\"maxStudents\":50}",
            "{\"classCode\":\"INT2204-01\",\"subjectId\":10,\"semesterId\":0,\"lecturerId\":51,\"maxStudents\":50}",
            "{\"classCode\":\"INT2204-01\",\"subjectId\":10,\"semesterId\":5,\"lecturerId\":0,\"maxStudents\":50}",
            "{\"classCode\":\"INT2204-01\",\"subjectId\":10,\"semesterId\":5,\"lecturerId\":51,\"maxStudents\":0}",
            "{\"classCode\":\"INT2204-01\",\"subjectId\":10,\"semesterId\":5,\"lecturerId\":51,\"maxStudents\":-1}"
    })
    void rejectsInvalidPayloadBeforeAccessingRepositories(String body) throws Exception {
        mockMvc.perform(post("/api/admin/classes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseClassRepository, subjectRepository, semesterRepository, lecturerRepository);
    }

    @Test
    void returnsErrorResponseWhenSaveFails() throws Exception {
        when(courseClassRepository.save(any(CourseClass.class)))
                .thenThrow(new DataAccessResourceFailureException("Database unavailable"));

        mockMvc.perform(post("/api/admin/classes").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("error to add new course class"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
