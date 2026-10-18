package com.example.oopPTIT.controller;

import com.example.oopPTIT.exception.StudentExceptionHandler;
import com.example.oopPTIT.repository.StudentRepository;
import com.example.oopPTIT.repository.UserRepository;
import com.example.oopPTIT.util.Role;
import com.example.oopPTIT.util.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:create-student;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.sql.init.mode=never"
})
class CreateStudentIntegrationTests {

    @Autowired private AdminStudentController controller;
    @Autowired private StudentRepository students;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbc;
    private MockMvc mvc;

    private static final String BODY = """
            {"username":"student01","password":"DemoPassword123!",
             "email":"student01@example.com","studentCode":"B23DCCN001",
             "fullName":"Nguyen Van An","dateOfBirth":"2005-01-01","phone":"0901234567"}
            """;

    @BeforeEach
    void setUp() {
        students.deleteAll();
        users.deleteAll();
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new StudentExceptionHandler()).build();
    }

    @Test
    void createsLinkedAccountAndProfileWithHashedPassword() throws Exception {
        mvc.perform(post("/api/admin/students").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").isNumber())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.studentCode").value("B23DCCN001"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.user").doesNotExist());

        assertThat(users.count()).isEqualTo(1);
        assertThat(students.count()).isEqualTo(1);
        var user = users.findAll().getFirst();
        var student = students.findAll().getFirst();
        assertThat(student.getUser().getId()).isEqualTo(user.getId());
        assertThat(user.getRole()).isEqualTo(Role.STUDENT);
        assertThat(user.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(user.getPassword()).isNotEqualTo("DemoPassword123!");
        assertThat(passwordEncoder.matches("DemoPassword123!", user.getPassword())).isTrue();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(student.getCreatedAt()).isNotNull();
        assertThat(student.getUpdatedAt()).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"username", "email", "studentCode", "phone"})
    void rejectsEachDuplicateField(String field) throws Exception {
        mvc.perform(post("/api/admin/students").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());
        String second = """
                {"username":"student02","password":"DemoPassword123!",
                 "email":"student02@example.com","studentCode":"B23DCCN002",
                 "fullName":"Tran Van Binh","dateOfBirth":"2005-01-01","phone":"0901234568"}
                """;
        second = switch (field) {
            case "username" -> second.replace("student02\"", "STUDENT01\"");
            case "email" -> second.replace("student02@example.com", "STUDENT01@example.com");
            case "studentCode" -> second.replace("B23DCCN002", "b23dccn001");
            default -> second.replace("0901234568", "0901234567");
        };
        mvc.perform(post("/api/admin/students").contentType(MediaType.APPLICATION_JSON).content(second))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").isNumber())
                .andExpect(jsonPath("$.status").value(409));
        assertThat(users.count()).isEqualTo(1);
        assertThat(students.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{", "{\"dateOfBirth\":\"invalid\"}"})
    void rejectsInvalidBodyWithoutWritingData(String body) throws Exception {
        mvc.perform(post("/api/admin/students").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        assertThat(users.count()).isZero();
        assertThat(students.count()).isZero();
    }

    @Test
    void rollsBackAccountWhenDatabaseRejectsProfile() throws Exception {
        jdbc.execute("ALTER TABLE students ADD CONSTRAINT reject_test_student CHECK (student_code <> 'B23DCCN001')");
        try {
            mvc.perform(post("/api/admin/students").contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isConflict());
            assertThat(users.count()).isZero();
            assertThat(students.count()).isZero();
        } finally {
            jdbc.execute("ALTER TABLE students DROP CONSTRAINT reject_test_student");
        }
    }
}
