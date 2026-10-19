package com.example.oopPTIT.controller;

import com.example.oopPTIT.exception.StudentExceptionHandler;
import com.example.oopPTIT.repository.*;
import com.example.oopPTIT.util.Role;
import com.example.oopPTIT.util.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-profiles;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false", "spring.sql.init.mode=never"
})
class AdminProfileIntegrationTests {
    @Autowired private AdminStudentController studentController;
    @Autowired private AdminLecturerController lecturerController;
    @Autowired private StudentRepository students;
    @Autowired private LecturerRepository lecturers;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JdbcTemplate jdbc;
    private MockMvc mvc;

    private static final String STUDENT = """
            {"username":"student01","password":"DemoPassword123!","email":"student@example.com",
             "studentCode":"SV01","fullName":"Student One","dateOfBirth":"2005-01-01","phone":"0901234567"}
            """;
    private static final String LECTURER = """
            {"username":"lecturer01","password":"DemoPassword123!","email":"lecturer@example.com",
             "lecturerCode":"GV01","fullName":"Lecturer One","department":"CNTT","phone":"0901234568"}
            """;

    @BeforeEach
    void setUp() {
        students.deleteAll();
        lecturers.deleteAll();
        users.deleteAll();
        mvc = MockMvcBuilders.standaloneSetup(studentController, lecturerController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new StudentExceptionHandler()).build();
    }

    @Test
    void readsAndPartiallyUpdatesStudentAndAccountStatus() throws Exception {
        mvc.perform(post("/api/admin/students").contentType(MediaType.APPLICATION_JSON).content(STUDENT))
                .andExpect(status().isCreated());
        Long id = students.findAll().getFirst().getId();
        String path = "/api/admin/students/" + id;
        mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("student@example.com"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
        mvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\" Updated Name \",\"email\":\"new@example.com\",\"phone\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.data.phone").value("0901234567"));
        assertThat(students.findById(id).orElseThrow().getFullName()).isEqualTo("Updated Name");
        assertThat(users.findAll().getFirst().getEmail()).isEqualTo("new@example.com");
        for (String value : new String[]{"INACTIVE", "ACTIVE"}) {
            mvc.perform(patch(path + "/status").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"" + value + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(value));
            assertThat(users.findAll().getFirst().getStatus()).isEqualTo(Status.valueOf(value));
        }
        mvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentCode\":\"SV01\",\"phone\":\"0901234567\",\"email\":\"new@example.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void createsListsAndUpdatesLecturerWithEncodedPassword() throws Exception {
        mvc.perform(get("/api/admin/lecturers")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPage").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty());
        mvc.perform(post("/api/admin/lecturers").contentType(MediaType.APPLICATION_JSON).content(LECTURER))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
        var user = users.findAll().getFirst();
        assertThat(user.getRole()).isEqualTo(Role.LECTURER);
        assertThat(encoder.matches("DemoPassword123!", user.getPassword())).isTrue();
        Long id = lecturers.findAll().getFirst().getId();
        mvc.perform(patch("/api/admin/lecturers/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"department\":\" Updated Department \",\"email\":\"updated@example.com\",\"lecturerCode\":\"GV01\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.fullName").value("Lecturer One"));
        mvc.perform(get("/api/admin/lecturers")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].department").value("Updated Department"))
                .andExpect(jsonPath("$.data.items[0].email").value("updated@example.com"));
        assertThat(lecturers.findById(id).orElseThrow().getDepartment()).isEqualTo("Updated Department");
    }

    @Test
    void readsRequestedLecturerByIdWithoutExposingPasswordOrRelationships() throws Exception {
        mvc.perform(post("/api/admin/lecturers").contentType(MediaType.APPLICATION_JSON).content(LECTURER))
                .andExpect(status().isCreated());
        String second = """
                {"username":"lecturer02","password":"DemoPassword123!","email":"second@example.com",
                 "lecturerCode":"GV02","fullName":"Lecturer Two","department":"DTVT","phone":"0901234569"}
                """;
        mvc.perform(post("/api/admin/lecturers").contentType(MediaType.APPLICATION_JSON).content(second))
                .andExpect(status().isCreated());
        Long id = lecturers.findAll().stream()
                .filter(lecturer -> lecturer.getLecturerCode().equals("GV02"))
                .findFirst().orElseThrow().getId();

        mvc.perform(get("/api/admin/lecturers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").isNumber())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Lecturer retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(id.intValue()))
                .andExpect(jsonPath("$.data.lecturerCode").value("GV02"))
                .andExpect(jsonPath("$.data.fullName").value("Lecturer Two"))
                .andExpect(jsonPath("$.data.department").value("DTVT"))
                .andExpect(jsonPath("$.data.phone").value("0901234569"))
                .andExpect(jsonPath("$.data.username").value("lecturer02"))
                .andExpect(jsonPath("$.data.email").value("second@example.com"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.user").doesNotExist())
                .andExpect(jsonPath("$.data.courseClasses").doesNotExist());
    }

    @Test
    void returnsNotFoundWhenLecturerIdDoesNotExist() throws Exception {
        mvc.perform(get("/api/admin/lecturers/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").isNumber())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Lecturer not found: 999999"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "1.5", "9223372036854775808"})
    void rejectsMalformedLecturerIds(String id) throws Exception {
        mvc.perform(get("/api/admin/lecturers/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").isNumber())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void rejectsMissingResourcesAndInvalidRequests() throws Exception {
        mvc.perform(get("/api/admin/students/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/students/invalid")).andExpect(status().isBadRequest());
        for (String path : new String[]{"/api/admin/students/999999", "/api/admin/lecturers/999999"}) {
            mvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isNotFound());
            mvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\" \"}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(patch("/api/admin/students/999999/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isNotFound());
        for (String body : new String[]{"{}", "{\"status\":null}", "{\"status\":\"UNKNOWN\"}"}) {
            mvc.perform(patch("/api/admin/students/999999/status").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/admin/lecturers").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsDuplicatesAndRollsBackPartialUpdates() throws Exception {
        mvc.perform(post("/api/admin/students").contentType(MediaType.APPLICATION_JSON).content(STUDENT))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/admin/lecturers").contentType(MediaType.APPLICATION_JSON).content(LECTURER))
                .andExpect(status().isCreated());
        String second = LECTURER.replace("lecturer01", "lecturer02").replace("lecturer@example.com", "second@example.com")
                .replace("GV01", "GV02");
        for (String duplicate : new String[]{second.replace("lecturer02", "LECTURER01"),
                second.replace("second@example.com", "STUDENT@example.com"), second.replace("GV02", "gv01")}) {
            mvc.perform(post("/api/admin/lecturers").contentType(MediaType.APPLICATION_JSON).content(duplicate))
                    .andExpect(status().isConflict());
        }
        mvc.perform(patch("/api/admin/students/" + students.findAll().getFirst().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentCode\":\"CHANGED\",\"email\":\"LECTURER@example.com\"}"))
                .andExpect(status().isConflict());
        assertThat(students.findAll().getFirst().getStudentCode()).isEqualTo("SV01");
        mvc.perform(patch("/api/admin/lecturers/" + lecturers.findAll().getFirst().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lecturerCode\":\"CHANGED\",\"email\":\"STUDENT@example.com\"}"))
                .andExpect(status().isConflict());
        assertThat(lecturers.findAll().getFirst().getLecturerCode()).isEqualTo("GV01");
        assertThat(users.count()).isEqualTo(2);
    }

    @Test
    void rollsBackAccountWhenLecturerProfileCannotBeSaved() throws Exception {
        jdbc.execute("ALTER TABLE lecturers ADD CONSTRAINT reject_lecturer CHECK (lecturer_code <> 'GV01')");
        try {
            mvc.perform(post("/api/admin/lecturers").contentType(MediaType.APPLICATION_JSON).content(LECTURER))
                    .andExpect(status().isConflict());
            assertThat(users.count()).isZero();
            assertThat(lecturers.count()).isZero();
        } finally {
            jdbc.execute("ALTER TABLE lecturers DROP CONSTRAINT reject_lecturer");
        }
    }
}
