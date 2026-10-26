package com.example.oopPTIT.controller;

import com.example.oopPTIT.SecurityTestSupport;
import org.springframework.security.test.context.support.WithMockUser;

import com.example.oopPTIT.entity.CourseClass;
import com.example.oopPTIT.entity.Lecturer;
import com.example.oopPTIT.entity.Semester;
import com.example.oopPTIT.entity.Subject;
import com.example.oopPTIT.entity.User;
import com.example.oopPTIT.repository.CourseClassRepository;
import com.example.oopPTIT.repository.LecturerRepository;
import com.example.oopPTIT.repository.SemesterRepository;
import com.example.oopPTIT.repository.SubjectRepository;
import com.example.oopPTIT.repository.UserRepository;
import com.example.oopPTIT.util.Role;
import com.example.oopPTIT.util.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assign-lecturer;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.sql.init.mode=never"
})
@WithMockUser(roles = "ADMIN")
class AssignLecturerIntegrationTests extends SecurityTestSupport {
    @Autowired private AdminCourseClassController controller;
    @Autowired private CourseClassRepository classes;
    @Autowired private LecturerRepository lecturers;
    @Autowired private UserRepository users;
    @Autowired private SubjectRepository subjects;
    @Autowired private SemesterRepository semesters;
    private MockMvc mvc;
    private CourseClass courseClass;
    private CourseClass otherClass;
    private Lecturer originalLecturer;
    private Lecturer replacement;

    @BeforeEach
    void setUp() {
        classes.deleteAll();
        lecturers.deleteAll();
        users.deleteAll();
        subjects.deleteAll();
        semesters.deleteAll();
        mvc = MockMvcBuilders.standaloneSetup(controller).build();

        Subject subject = new Subject();
        subject.setSubjectCode("INT2204");
        subject.setSubjectName("Object Oriented Programming");
        subject.setCredits(3);
        subject = subjects.saveAndFlush(subject);
        Semester semester = new Semester();
        semester.setName("2026-1");
        semester = semesters.saveAndFlush(semester);

        originalLecturer = saveLecturer("GV01");
        replacement = saveLecturer("GV02");
        courseClass = saveClass("INT2204-01", subject, semester);
        otherClass = saveClass("INT2204-02", subject, semester);
    }

    @Test
    void persistsLecturerChangeWithoutModifyingOtherClassData() throws Exception {
        mvc.perform(patch("/api/admin/classes/{id}/lecturer", courseClass.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lecturerId\":" + replacement.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(courseClass.getId().intValue()))
                .andExpect(jsonPath("$.data.lecturerId").value(replacement.getId().intValue()))
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());

        CourseClass saved = classes.findById(courseClass.getId()).orElseThrow();
        assertThat(saved.getLecturer().getId()).isEqualTo(replacement.getId());
        assertThat(saved.getClassCode()).isEqualTo(courseClass.getClassCode());
        assertThat(saved.getMaxStudents()).isEqualTo(courseClass.getMaxStudents());
        assertThat(saved.getStatus()).isEqualTo(courseClass.getStatus());
        assertThat(saved.getSubject().getId()).isEqualTo(courseClass.getSubject().getId());
        assertThat(saved.getSemester().getId()).isEqualTo(courseClass.getSemester().getId());
        assertThat(saved.getCreatedAt()).isEqualTo(courseClass.getCreatedAt());
        assertThat(saved.getUpdatedAt()).isAfterOrEqualTo(courseClass.getUpdatedAt());
        assertThat(classes.findById(otherClass.getId()).orElseThrow().getLecturer().getId())
                .isEqualTo(originalLecturer.getId());
        assertThat(classes.count()).isEqualTo(2);
        assertThat(lecturers.count()).isEqualTo(2);
    }

    @Test
    void repeatedAssignmentOfSameLecturerSucceedsWithoutCreatingRecords() throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(patch("/api/admin/classes/{id}/lecturer", courseClass.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"lecturerId\":" + originalLecturer.getId() + "}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.lecturerId").value(originalLecturer.getId().intValue()));
        }

        assertThat(classes.findById(courseClass.getId()).orElseThrow().getLecturer().getId())
                .isEqualTo(originalLecturer.getId());
        assertThat(classes.count()).isEqualTo(2);
        assertThat(lecturers.count()).isEqualTo(2);
    }

    @Test
    void missingLecturerLeavesPersistedAssignmentUnchanged() throws Exception {
        mvc.perform(patch("/api/admin/classes/{id}/lecturer", courseClass.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lecturerId\":999999}"))
                .andExpect(status().isNotFound());

        assertThat(classes.findById(courseClass.getId()).orElseThrow().getLecturer().getId())
                .isEqualTo(originalLecturer.getId());
    }

    @Test
    void missingClassReturns404WithoutCreatingAClass() throws Exception {
        mvc.perform(patch("/api/admin/classes/999999/lecturer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lecturerId\":" + replacement.getId() + "}"))
                .andExpect(status().isNotFound());

        assertThat(classes.count()).isEqualTo(2);
    }

    private Lecturer saveLecturer(String code) {
        User user = new User();
        user.setUsername(code);
        user.setEmail(code + "@example.test");
        user.setPassword("unused-test-hash");
        user.setRole(Role.LECTURER);
        user.setStatus(Status.ACTIVE);
        user = users.saveAndFlush(user);

        Lecturer lecturer = new Lecturer();
        lecturer.setLecturerCode(code);
        lecturer.setFullName("Lecturer " + code);
        lecturer.setDepartment("CNTT");
        lecturer.setPhone("0901234567");
        lecturer.setUser(user);
        return lecturers.saveAndFlush(lecturer);
    }

    private CourseClass saveClass(String code, Subject subject, Semester semester) {
        CourseClass entity = new CourseClass();
        entity.setClassCode(code);
        entity.setMaxStudents(50);
        entity.setStatus(Status.ACTIVE);
        entity.setSubject(subject);
        entity.setSemester(semester);
        entity.setLecturer(originalLecturer);
        return classes.saveAndFlush(entity);
    }
}
