package com.example.oopPTIT.security;

import com.example.oopPTIT.SecurityTestSupport;
import com.example.oopPTIT.dto.request.CreateClassSessionRequest;
import com.example.oopPTIT.dto.request.ManualAttendanceRequest;
import com.example.oopPTIT.dto.request.OpenAttendanceSessionRequest;
import com.example.oopPTIT.dto.request.RecordAttendanceRequest;
import com.example.oopPTIT.dto.request.UpdateAttendanceRequest;
import com.example.oopPTIT.entity.*;
import com.example.oopPTIT.repository.*;
import com.example.oopPTIT.service.AdminUserService;
import com.example.oopPTIT.service.AttendanceService;
import com.example.oopPTIT.service.ClassSessionService;
import com.example.oopPTIT.service.CourseClassService;
import com.example.oopPTIT.util.Role;
import com.example.oopPTIT.util.SessionStatus;
import com.example.oopPTIT.util.Status;
import com.example.oopPTIT.util.TimeStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:jwt-security;MODE=MySQL;DB_CLOSE_DELAY=-1")
class JwtSecurityIntegrationTests extends SecurityTestSupport {
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final String HASH = new CompatiblePasswordEncoder().encode(PASSWORD);
    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper json;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private UserRepository users;
    @Autowired private AttendanceRepository attendances;
    @Autowired private AttendanceSessionRepository sessions;
    @Autowired private ClassSessionRepository classSessions;
    @Autowired private JwtEncoder encoder;
    @Autowired private JwtDecoder decoder;
    @Autowired private JwtProperties jwtProperties;
    @Autowired private CompatiblePasswordEncoder passwords;
    @Autowired private DatabaseJwtAuthenticationConverter converter;
    @Autowired private AdminUserService adminUserService;
    @Autowired private AttendanceService attendanceService;
    @Autowired private ClassSessionService classSessionService;
    @Autowired private CourseClassService courseClassService;
    private MockMvc mvc;
    private TransactionTemplate transactions;
    private User studentAccount;
    private User adminAccount;
    private Student student;
    private Student otherStudent;
    private CourseClass ownedClass;
    private CourseClass otherClass;
    private AttendanceSession ownedSession;
    private AttendanceSession otherSession;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        transactions = new TransactionTemplate(transactionManager);
        transactions.executeWithoutResult(transaction -> {
            for (String entity : List.of("Attendance", "AttendanceSession", "ClassSession", "StudentHasCourseClass",
                    "CourseClass", "Student", "Lecturer", "User", "Subject", "Semester")) {
                entityManager.createQuery("delete from " + entity).executeUpdate();
            }
            adminAccount = user("admin", Role.ADMIN);
            studentAccount = user("student", Role.STUDENT);
            student = student(studentAccount, "SV1", "0900000001");
            otherStudent = student(user("other-student", Role.STUDENT), "SV2", "0900000002");
            Lecturer lecturer = lecturer(user("lecturer", Role.LECTURER), "GV1");
            Lecturer otherLecturer = lecturer(user("other-lecturer", Role.LECTURER), "GV2");
            Subject subject = new Subject();
            subject.setSubjectCode("INT001");
            subject.setSubjectName("Security");
            subject.setCredits(3);
            entityManager.persist(subject);
            Semester semester = new Semester();
            semester.setName("2026-1");
            entityManager.persist(semester);
            ownedClass = courseClass("INT001-01", lecturer, subject, semester);
            otherClass = courseClass("INT001-02", otherLecturer, subject, semester);
            ownedSession = attendanceSession(ownedClass, 1);
            otherSession = attendanceSession(otherClass, 1);
            entityManager.persist(StudentHasCourseClass.builder().courseClass(ownedClass).student(student).status(Status.ACTIVE).build());
            entityManager.persist(StudentHasCourseClass.builder().courseClass(otherClass).student(otherStudent).status(Status.ACTIVE).build());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"student", "student@example.test", " STUDENT@EXAMPLE.TEST "})
    void logsInWithUsernameOrEmailAndReturnsSafeClaims(String identifier) throws Exception {
        String token = login(identifier, PASSWORD);
        Jwt jwt = decoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo(studentAccount.getId().toString());
        assertThat(jwt.getClaimAsString("username")).isEqualTo("student");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("STUDENT");
        assertThat(jwt.getClaims()).doesNotContainKeys("password", "email", "credentials");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.userId").value(studentAccount.getId().intValue()))
                .andExpect(jsonPath("$.data.username").value("student"))
                .andExpect(jsonPath("$.data.password").doesNotExist()).andExpect(cookie().doesNotExist("JSESSIONID"));
    }

    @Test
    void badPasswordUnknownUserAndInactiveAccountHaveSameUnauthorizedResponse() throws Exception {
        String incorrect = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("student", "incorrect")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials("missing", PASSWORD)))
                .andExpect(status().isUnauthorized()).andExpect(content().json(incorrect));
        studentAccount.setStatus(Status.INACTIVE);
        users.saveAndFlush(studentAccount);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials("student", PASSWORD)))
                .andExpect(status().isUnauthorized()).andExpect(content().json(incorrect));
    }

    @Test
    void authenticatesAndUpgradesExistingPbkdf2Hash() throws Exception {
        studentAccount.setPassword(Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode(PASSWORD));
        users.saveAndFlush(studentAccount);
        login("student", PASSWORD);
        String upgraded = users.findById(studentAccount.getId()).orElseThrow().getPassword();
        assertThat(upgraded).startsWith("$2");
        assertThat(passwords.matches(PASSWORD, upgraded)).isTrue();
    }

    @Test
    void preservesLongLegacyPasswordWithoutBcryptTruncation() throws Exception {
        String longPassword = PASSWORD.repeat(3);
        String hash = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode(longPassword);
        studentAccount.setPassword(hash);
        users.saveAndFlush(studentAccount);
        login("student", longPassword);
        assertThat(users.findById(studentAccount.getId()).orElseThrow().getPassword()).isEqualTo(hash);
        assertThatThrownBy(() -> passwords.encode(longPassword)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesAmbiguousUsernameEmailIdentity() throws Exception {
        transactions.executeWithoutResult(transaction -> user("student@example.test", Role.ADMIN));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("student@example.test", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/me", "/api/admin/users", "/api/admin/students", "/api/admin/lecturers",
            "/api/admin/classes", "/api/admin/subjects", "/api/admin/semesters", "/api/lecturer/classes",
            "/api/student/profile", "/api/student/attendance", "/api/students/me/classes", "/api/classes/1/sessions",
            "/api/sessions/1/attendance", "/api/lecturers/me/classes"})
    void requiresJwtForEveryProtectedEndpoint(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.trace").doesNotExist()).andExpect(cookie().doesNotExist("JSESSIONID"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"expired", "future", "future-iat", "issuer", "audience", "no-expiry", "no-nbf", "no-iat", "bad-subject",
            "wrong-user-id", "wrong-username", "elevated-role", "missing-roles", "string-roles"})
    void rejectsInvalidJwtClaims(String variation) throws Exception {
        JwtClaimsSet.Builder claims = claims();
        switch (variation) {
            case "expired" -> claims.issuedAt(Instant.now().minusSeconds(120)).notBefore(Instant.now().minusSeconds(120))
                    .expiresAt(Instant.now().minusSeconds(1));
            case "future" -> claims.notBefore(Instant.now().plusSeconds(120));
            case "future-iat" -> claims.issuedAt(Instant.now().plusSeconds(120));
            case "issuer" -> claims.issuer("another-issuer");
            case "audience" -> claims.audience(List.of("another-api"));
            case "no-expiry" -> claims.claims(map -> map.remove("exp"));
            case "no-nbf" -> claims.claims(map -> map.remove("nbf"));
            case "no-iat" -> claims.claims(map -> map.remove("iat"));
            case "bad-subject" -> claims.subject("not-an-id");
            case "wrong-user-id" -> claims.claim("userId", adminAccount.getId());
            case "wrong-username" -> claims.claim("username", "admin");
            case "elevated-role" -> claims.claim("roles", List.of("ADMIN"));
            case "missing-roles" -> claims.claims(map -> map.remove("roles"));
            case "string-roles" -> claims.claim("roles", "ADMIN");
        }
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + signed(encoder, claims.build())))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void rejectsWrongSignatureMalformedAndUnsignedTokens() throws Exception {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        var wrongEncoder = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(key, "HmacSHA256"))
                .algorithm(MacAlgorithm.HS256).build();
        String unsigned = new com.nimbusds.jwt.PlainJWT(com.nimbusds.jwt.JWTClaimsSet.parse(
                "{\"sub\":\"" + studentAccount.getId() + "\"}")).serialize();
        for (String token : List.of(signed(wrongEncoder, claims().build()), "not-a-jwt", unsigned)) {
            mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        }
    }

    @ParameterizedTest
    @CsvSource({"student,/api/admin/users", "lecturer,/api/admin/users", "student,/api/lecturer/classes",
            "lecturer,/api/student/profile", "lecturer,/api/student/attendance", "lecturer,/api/students/me/classes",
            "student,/api/classes/1/sessions", "student,/api/lecturers/me/classes",
            "admin,/api/lecturers/me/classes"})
    void enforcesRoleBoundaries(String username, String path) throws Exception {
        mvc.perform(get(path).header("Authorization", "Bearer " + login(username, PASSWORD)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminCanListUsersAndUseExistingAdministrativeApis() throws Exception {
        String token = login("admin", PASSWORD);
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(5))
                .andExpect(jsonPath("$.data.items[*].password").doesNotExist());
        mvc.perform(get("/api/admin/students/{id}", otherStudent.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.studentCode").value("SV2"));
    }

    @Test
    void lecturerCanListOnlySessionsInAssignedClass() throws Exception {
        ClassSession session = ownedSession.getClassSession();
        mvc.perform(get("/api/classes/{id}/sessions", ownedClass.getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD))
                        .param("classId", otherClass.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.pageNo").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalPage").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(session.getId().intValue()))
                .andExpect(jsonPath("$.data.items[0].classId").value(ownedClass.getId().intValue()))
                .andExpect(jsonPath("$.data.items[0].sessionNo").value(1))
                .andExpect(jsonPath("$.data.items[0].sessionDate").value(session.getSessionDate().toString()))
                .andExpect(jsonPath("$.data.items[0].startTime").value("08:00:00"))
                .andExpect(jsonPath("$.data.items[0].endTime").value("10:00:00"))
                .andExpect(jsonPath("$.data.items[0].createdAt").exists())
                .andExpect(jsonPath("$.data.items[0].updatedAt").exists())
                .andExpect(jsonPath("$.data.items[0].courseClass").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].lecturer").doesNotExist());
    }

    @Test
    void adminCanListSessionsFromAnyClass() throws Exception {
        String token = login("admin", PASSWORD);
        for (CourseClass courseClass : List.of(ownedClass, otherClass)) {
            mvc.perform(get("/api/classes/{id}/sessions", courseClass.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items.length()").value(1))
                    .andExpect(jsonPath("$.data.items[0].classId").value(courseClass.getId().intValue()));
        }
    }

    @Test
    void paginatesAndSortsClassSessionsWithoutMixingClasses() throws Exception {
        transactions.executeWithoutResult(transaction -> {
            classSession(ownedClass, 3);
            classSession(ownedClass, 4);
        });
        String token = login("lecturer", PASSWORD);
        mvc.perform(get("/api/classes/{id}/sessions", ownedClass.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].sessionNo").value(contains(1, 3, 4)));
        mvc.perform(get("/api/classes/{id}/sessions", ownedClass.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1").param("size", "1").param("sort", "sessionNo,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(1))
                .andExpect(jsonPath("$.data.totalPage").value(3))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].sessionNo").value(3));
    }

    @Test
    void returnsEmptyPageForExistingClassWithoutSessions() throws Exception {
        Long emptyClassId = transactions.execute(transaction -> {
            CourseClass managed = entityManager.find(CourseClass.class, ownedClass.getId());
            return courseClass("EMPTY-CLASS", managed.getLecturer(), managed.getSubject(), managed.getSemester()).getId();
        });
        mvc.perform(get("/api/classes/{id}/sessions", emptyClassId)
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPage").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void returnsEmptyPageWhenClassSessionPageIsPastTheEnd() throws Exception {
        mvc.perform(get("/api/classes/{id}/sessions", ownedClass.getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD))
                        .param("page", "5").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(5))
                .andExpect(jsonPath("$.data.totalPage").value(1))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void lecturerCannotReadSessionsFromUnassignedClass() throws Exception {
        mvc.perform(get("/api/classes/{id}/sessions", otherClass.getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void assignedLecturerCanCreateClassSessionWithoutOpeningAttendance() throws Exception {
        String body = json.writeValueAsString(newSessionRequest());
        String response = mvc.perform(post("/api/classes/{id}/sessions", ownedClass.getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD))
                        .param("classId", otherClass.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.classId").value(ownedClass.getId().intValue()))
                .andExpect(jsonPath("$.data.sessionNo").value(2))
                .andExpect(jsonPath("$.data.sessionDate").value(newSessionRequest().getSessionDate().toString()))
                .andExpect(jsonPath("$.data.startTime").value("09:00:00"))
                .andExpect(jsonPath("$.data.endTime").value("11:00:00"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists())
                .andExpect(jsonPath("$.data.courseClass").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        Long id = json.readTree(response).get("data").get("id").asLong();
        assertThat(classSessions.findById(id)).isPresent();
        assertThat(classSessions.count()).isEqualTo(3);
        assertThat(sessions.existsByClassSession_Id(id)).isFalse();
    }

    @Test
    void adminCreatesAnIndependentSessionSequenceForEachClass() throws Exception {
        String token = login("admin", PASSWORD);
        for (CourseClass courseClass : List.of(ownedClass, otherClass)) {
            mvc.perform(post("/api/classes/{id}/sessions", courseClass.getId())
                            .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(newSessionRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.classId").value(courseClass.getId().intValue()))
                    .andExpect(jsonPath("$.data.sessionNo").value(2));
        }
        assertThat(classSessions.count()).isEqualTo(4);
    }

    @Test
    void cannotCreateClassSessionWithoutJwt() throws Exception {
        mvc.perform(post("/api/classes/{id}/sessions", ownedClass.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(newSessionRequest())))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        assertThat(classSessions.count()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"student", "other-lecturer"})
    void studentsAndUnassignedLecturersCannotCreateClassSessions(String username) throws Exception {
        mvc.perform(post("/api/classes/{id}/sessions", ownedClass.getId())
                        .header("Authorization", "Bearer " + login(username, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(newSessionRequest())))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        assertThat(classSessions.count()).isEqualTo(2);
    }

    @Test
    void duplicateClassSessionTimeSlotReturnsConflictWithoutCreatingAnotherRecord() throws Exception {
        String token = login("lecturer", PASSWORD);
        String body = json.writeValueAsString(newSessionRequest());
        mvc.perform(post("/api/classes/{id}/sessions", ownedClass.getId())
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/classes/{id}/sessions", ownedClass.getId())
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        assertThat(classSessions.count()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "{broken-json",
            "{\"startTime\":\"09:00\",\"endTime\":\"11:00\"}",
            "{\"sessionDate\":\"2026-10-28\",\"endTime\":\"11:00\"}",
            "{\"sessionDate\":\"2026-10-28\",\"startTime\":\"09:00\"}",
            "{\"sessionDate\":\"2026-10-28\",\"startTime\":\"09:00\",\"endTime\":\"09:00\"}",
            "{\"sessionDate\":\"2026-10-28\",\"startTime\":\"11:00\",\"endTime\":\"09:00\"}",
            "{\"sessionDate\":\"invalid\",\"startTime\":\"09:00\",\"endTime\":\"11:00\"}",
            "{\"sessionDate\":\"2026-10-28\",\"startTime\":\"invalid\",\"endTime\":\"11:00\"}"})
    void classSessionCreationRejectsInvalidBodies(String body) throws Exception {
        mvc.perform(post("/api/classes/{id}/sessions", ownedClass.getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        assertThat(classSessions.count()).isEqualTo(2);
    }

    @Test
    void creatingSessionForMissingOrMalformedClassReturnsAppropriateError() throws Exception {
        String token = login("admin", PASSWORD);
        String body = json.writeValueAsString(newSessionRequest());
        mvc.perform(post("/api/classes/{id}/sessions", Long.MAX_VALUE)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mvc.perform(post("/api/classes/not-a-number/sessions")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        assertThat(classSessions.count()).isEqualTo(2);
    }

    @Test
    void sessionNumberExhaustionReturnsConflictInsteadOfOverflow() throws Exception {
        transactions.executeWithoutResult(transaction -> classSession(ownedClass, Integer.MAX_VALUE));
        mvc.perform(post("/api/classes/{id}/sessions", ownedClass.getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(newSessionRequest())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        assertThat(classSessions.count()).isEqualTo(3);
    }

    @Test
    void concurrentRequestsCannotCreateTheSameClassSessionTwice() throws Exception {
        String token = login("lecturer", PASSWORD);
        String body = json.writeValueAsString(newSessionRequest());
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> create = () -> {
            assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
            return mvc.perform(post("/api/classes/{id}/sessions", ownedClass.getId())
                            .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andReturn().getResponse().getStatus();
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(create);
            var second = executor.submit(create);
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(classSessions.count()).isEqualTo(3);
    }

    @Test
    void concurrentRequestsForDifferentClassesAllocateIndependentSessionNumbers() throws Exception {
        String token = login("admin", PASSWORD);
        String body = json.writeValueAsString(newSessionRequest());
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = List.of(ownedClass, otherClass).stream().map(courseClass -> executor.submit(() -> {
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                return mvc.perform(post("/api/classes/{id}/sessions", courseClass.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                        .andReturn().getResponse().getStatus();
            })).toList();
            start.countDown();
            for (var result : results) assertThat(result.get(20, TimeUnit.SECONDS)).isEqualTo(201);
        }
        assertThat(classSessions.findAll()).extracting(ClassSession::getSessionNo)
                .containsExactlyInAnyOrder(1, 1, 2, 2);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 15})
    void assignedLecturerCanOpenAttendanceAndReadItsStatus(int lateAfterMinutes) throws Exception {
        ClassSession classSession = unopenedClassSession(ownedClass);
        String token = login("lecturer", PASSWORD);
        mvc.perform(post("/api/sessions/{id}/attendance/open", classSession.getId())
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new OpenAttendanceSessionRequest(lateAfterMinutes))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.classSessionId").value(classSession.getId().intValue()))
                .andExpect(jsonPath("$.data.classId").value(ownedClass.getId().intValue()))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.openedAt").exists())
                .andExpect(jsonPath("$.data.closedAt").value(classSession.getSessionDate() + "T10:00:00"))
                .andExpect(jsonPath("$.data.lateAfterMinutes").value(lateAfterMinutes))
                .andExpect(jsonPath("$.data.classSession").doesNotExist())
                .andExpect(jsonPath("$.data.attendances").doesNotExist());
        AttendanceSession saved = sessions.findByClassSession_Id(classSession.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(SessionStatus.OPEN);
        assertThat(saved.getLateAfterMinutes()).isEqualTo(lateAfterMinutes);
        assertThat(saved.getClosedAt()).isEqualTo(classSession.getSessionDate().atTime(classSession.getEndTime()));
        assertThat(saved.getOpenedAt()).isBefore(saved.getClosedAt());
        assertThat(sessions.count()).isEqualTo(3);
        mvc.perform(get("/api/sessions/{id}/attendance", classSession.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(saved.getId().intValue()))
                .andExpect(jsonPath("$.data.classSessionId").value(classSession.getId().intValue()))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
        mvc.perform(post("/api/lecturer/classes/{id}/attendance", ownedClass.getId())
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(attendanceRequest(saved, student)))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @CsvSource({"OPEN,false,OPEN", "OPEN,true,CLOSED", "CLOSED,false,CLOSED"})
    void attendanceStatusRespectsExplicitClosureAndDeadline(SessionStatus storedStatus, boolean expired,
                                                            String expectedStatus) throws Exception {
        ownedSession.setStatus(storedStatus);
        if (expired) {
            ownedSession.setOpenedAt(LocalDateTime.now().minusHours(2));
            ownedSession.setClosedAt(LocalDateTime.now().minusHours(1));
        }
        sessions.saveAndFlush(ownedSession);
        mvc.perform(get("/api/sessions/{id}/attendance", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ownedSession.getId().intValue()))
                .andExpect(jsonPath("$.data.status").value(expectedStatus));
        assertThat(sessions.findById(ownedSession.getId()).orElseThrow().getStatus()).isEqualTo(storedStatus);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPEN", "CLOSED"})
    void cannotOpenAnExistingAttendanceSessionOrResetItsSettings(SessionStatus storedStatus) throws Exception {
        ownedSession.setStatus(storedStatus);
        sessions.saveAndFlush(ownedSession);
        AttendanceSession before = sessions.findById(ownedSession.getId()).orElseThrow();
        mvc.perform(post("/api/sessions/{id}/attendance/open", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lateAfterMinutes\":5}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        AttendanceSession after = sessions.findById(ownedSession.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(storedStatus);
        assertThat(after.getOpenedAt()).isEqualTo(before.getOpenedAt());
        assertThat(after.getClosedAt()).isEqualTo(before.getClosedAt());
        assertThat(after.getLateAfterMinutes()).isEqualTo(15);
        assertThat(sessions.count()).isEqualTo(2);
    }

    @Test
    void concurrentOpenRequestsCreateOnlyOneAttendanceSession() throws Exception {
        ClassSession classSession = unopenedClassSession(ownedClass);
        String token = login("lecturer", PASSWORD);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> open = () -> {
            assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
            return mvc.perform(post("/api/sessions/{id}/attendance/open", classSession.getId())
                            .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"lateAfterMinutes\":15}"))
                    .andReturn().getResponse().getStatus();
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(open);
            var second = executor.submit(open);
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(sessions.count()).isEqualTo(3);
        assertThat(sessions.existsByClassSession_Id(classSession.getId())).isTrue();
    }

    @Test
    void classSessionWithoutAttendanceReturnsNotFoundWithoutCreatingAnything() throws Exception {
        ClassSession classSession = unopenedClassSession(ownedClass);
        mvc.perform(get("/api/sessions/{id}/attendance", classSession.getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Attendance session has not been opened"));
        assertThat(sessions.existsByClassSession_Id(classSession.getId())).isFalse();
    }

    @Test
    void cannotOpenAttendanceAfterClassSessionHasEnded() throws Exception {
        ClassSession classSession = unopenedClassSession(ownedClass);
        transactions.executeWithoutResult(transaction -> entityManager.find(ClassSession.class, classSession.getId())
                .setSessionDate(LocalDate.now().minusDays(1)));
        mvc.perform(post("/api/sessions/{id}/attendance/open", classSession.getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lateAfterMinutes\":15}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        assertThat(sessions.existsByClassSession_Id(classSession.getId())).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "{broken-json", "{\"lateAfterMinutes\":null}", "{\"lateAfterMinutes\":-1}",
            "{\"lateAfterMinutes\":\"invalid\"}", "{\"lateAfterMinutes\":2147483648}"})
    void openingAttendanceValidatesTheExistingRequestDto(String body) throws Exception {
        mvc.perform(post("/api/sessions/{id}/attendance/open", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        assertThat(sessions.count()).isEqualTo(2);
    }

    @Test
    void openingAttendanceRequiresJwt() throws Exception {
        mvc.perform(post("/api/sessions/{id}/attendance/open", ownedSession.getClassSession().getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lateAfterMinutes\":15}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        assertThat(sessions.count()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"student", "admin"})
    void onlyLecturerRoleCanOpenOrReadAttendanceSessions(String username) throws Exception {
        String token = login(username, PASSWORD);
        Long classSessionId = ownedSession.getClassSession().getId();
        mvc.perform(post("/api/sessions/{id}/attendance/open", classSessionId)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lateAfterMinutes\":15}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc.perform(get("/api/sessions/{id}/attendance", classSessionId).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        assertThat(sessions.count()).isEqualTo(2);
    }

    @Test
    void lecturerCannotOpenOrReadAttendanceInUnassignedClassEvenWithForgedIdentity() throws Exception {
        String token = login("lecturer", PASSWORD);
        Long classSessionId = otherSession.getClassSession().getId();
        mvc.perform(post("/api/sessions/{id}/attendance/open", classSessionId)
                        .header("Authorization", "Bearer " + token).header("X-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("lateAfterMinutes", 15,
                                "classId", ownedClass.getId(), "lecturerId", ownedClass.getLecturer().getId()))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc.perform(get("/api/sessions/{id}/attendance", classSessionId).header("Authorization", "Bearer " + token)
                        .param("classId", ownedClass.getId().toString())
                        .param("lecturerId", ownedClass.getLecturer().getId().toString()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        assertThat(sessions.count()).isEqualTo(2);
    }

    @Test
    void reassigningClassRevokesAttendanceSessionAccessForPreviousLecturer() throws Exception {
        ClassSession classSession = unopenedClassSession(ownedClass);
        String oldLecturerToken = login("lecturer", PASSWORD);
        transactions.executeWithoutResult(transaction -> entityManager.find(CourseClass.class, ownedClass.getId())
                .setLecturer(entityManager.find(CourseClass.class, otherClass.getId()).getLecturer()));
        mvc.perform(post("/api/sessions/{id}/attendance/open", classSession.getId())
                        .header("Authorization", "Bearer " + oldLecturerToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lateAfterMinutes\":15}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/sessions/{id}/attendance", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + oldLecturerToken))
                .andExpect(status().isForbidden());
        String newLecturerToken = login("other-lecturer", PASSWORD);
        mvc.perform(post("/api/sessions/{id}/attendance/open", classSession.getId())
                        .header("Authorization", "Bearer " + newLecturerToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lateAfterMinutes\":15}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/sessions/{id}/attendance", classSession.getId())
                        .header("Authorization", "Bearer " + newLecturerToken))
                .andExpect(status().isOk());
    }

    @Test
    void missingClassSessionReturnsNotFoundForBothAttendanceSessionEndpoints() throws Exception {
        String token = login("lecturer", PASSWORD);
        mvc.perform(get("/api/sessions/{id}/attendance", Long.MAX_VALUE).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mvc.perform(post("/api/sessions/{id}/attendance/open", Long.MAX_VALUE)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lateAfterMinutes\":15}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        assertThat(sessions.count()).isEqualTo(2);
    }

    @Test
    void malformedClassSessionIdsReturnBadRequestForBothAttendanceSessionEndpoints() throws Exception {
        String token = login("lecturer", PASSWORD);
        mvc.perform(get("/api/sessions/not-a-number/attendance").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(post("/api/sessions/not-a-number/attendance/open").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lateAfterMinutes\":15}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void missingClassReturnsNotFoundWhenAdminListsSessions() throws Exception {
        mvc.perform(get("/api/classes/{id}/sessions", Long.MAX_VALUE)
                        .header("Authorization", "Bearer " + login("admin", PASSWORD)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void malformedClassIdForSessionListReturnsBadRequest() throws Exception {
        mvc.perform(get("/api/classes/not-a-number/sessions")
                        .header("Authorization", "Bearer " + login("admin", PASSWORD)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @ParameterizedTest
    @ValueSource(strings = {"notAField", "courseClass.lecturer.user.password"})
    void sessionListRejectsUnknownAndSensitiveSortFields(String field) throws Exception {
        mvc.perform(get("/api/classes/{id}/sessions", ownedClass.getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD))
                        .param("sort", field + ",asc"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void studentCannotAccessAnotherStudentsPrivateDetailsOrClassRoster() throws Exception {
        String token = login("student", PASSWORD);
        mvc.perform(get("/api/admin/students/{id}", otherStudent.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/classes/{id}/students", ownedClass.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/student/profile").header("Authorization", "Bearer " + token)
                        .param("studentId", otherStudent.getId().toString()).param("userId", adminAccount.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(student.getId().intValue()));
    }

    @Test
    void adminCanDeleteUnusedManagedEntitiesButReferencedEntitiesAreProtected() throws Exception {
        Map<String, Long> ids = transactions.execute(transaction -> {
            User studentUser = user("delete-student", Role.STUDENT);
            Student unusedStudent = student(studentUser, "DELETE-SV", "0900000098");
            User lecturerUser = user("delete-lecturer", Role.LECTURER);
            Lecturer unusedLecturer = lecturer(lecturerUser, "DELETE-GV");
            Subject unusedSubject = new Subject();
            unusedSubject.setSubjectCode("DELETE-HP");
            unusedSubject.setSubjectName("Delete subject");
            unusedSubject.setCredits(1);
            entityManager.persist(unusedSubject);
            Semester semester = entityManager.find(Semester.class, ownedClass.getSemester().getId());
            CourseClass unusedClass = courseClass("DELETE-CLASS", unusedLecturer, unusedSubject, semester);
            return Map.of("student", unusedStudent.getId(), "studentUser", studentUser.getId(),
                    "lecturer", unusedLecturer.getId(), "lecturerUser", lecturerUser.getId(),
                    "subject", unusedSubject.getId(), "courseClass", unusedClass.getId());
        });
        String token = login("admin", PASSWORD);

        mvc.perform(delete("/api/admin/classes/{id}", ids.get("courseClass"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/admin/lecturers/{id}", ids.get("lecturer"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/admin/subjects/{id}", ids.get("subject"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/admin/students/{id}", ids.get("student"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/admin/classes/{id}", ownedClass.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/admin/students/{id}", student.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        transactions.executeWithoutResult(transaction -> {
            assertThat(entityManager.find(CourseClass.class, ids.get("courseClass"))).isNull();
            assertThat(entityManager.find(Lecturer.class, ids.get("lecturer"))).isNull();
            assertThat(entityManager.find(Subject.class, ids.get("subject"))).isNull();
            assertThat(entityManager.find(Student.class, ids.get("student"))).isNull();
            assertThat(entityManager.find(User.class, ids.get("studentUser"))).isNull();
            assertThat(entityManager.find(User.class, ids.get("lecturerUser"))).isNull();
        });
    }

    @Test
    void studentOnlySeesOwnAttendanceAndEnrolledClasses() throws Exception {
        transactions.executeWithoutResult(transaction -> {
            attendance(student, ownedSession);
            attendance(otherStudent, otherSession);
        });
        String token = login("student", PASSWORD);
        mvc.perform(get("/api/student/attendance").header("Authorization", "Bearer " + token)
                        .param("studentId", otherStudent.getId().toString()).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.totalPage").value(1))
                .andExpect(jsonPath("$.data.items[0].studentId").value(student.getId().intValue()));
        mvc.perform(get("/api/students/me/classes").header("Authorization", "Bearer " + token)
                        .param("username", "other-student").param("studentId", otherStudent.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(ownedClass.getId().intValue()))
                .andExpect(jsonPath("$.data.items[0].subjectCode").value("INT001"))
                .andExpect(jsonPath("$.data.items[0].subjectName").value("Security"))
                .andExpect(jsonPath("$.data.items[0].semesterName").value("2026-1"))
                .andExpect(jsonPath("$.data.items[0].lecturerCode").value("GV1"))
                .andExpect(jsonPath("$.data.items[0].lecturerName").value("GV1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/lecturer/classes", "/api/lecturers/me/classes"})
    void lecturerCanOnlyListAssignedClassesAndTheirStudents(String path) throws Exception {
        String token = login("lecturer", PASSWORD);
        mvc.perform(get(path).header("Authorization", "Bearer " + token)
                        .param("username", "other-lecturer").param("lecturerId", otherClass.getLecturer().getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(ownedClass.getId().intValue()));
        mvc.perform(get(path).header("Authorization", "Bearer " + token)
                        .param("courseClass", "id:" + otherClass.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
        mvc.perform(get("/api/classes/{id}/students", ownedClass.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(1));
        mvc.perform(get("/api/classes/{id}/students", otherClass.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void lecturerSelfClassListSupportsPaginationSortingAndFilters() throws Exception {
        Long extraClassId = transactions.execute(transaction -> {
            CourseClass managed = entityManager.find(CourseClass.class, ownedClass.getId());
            return courseClass("INT001-03", managed.getLecturer(), managed.getSubject(), managed.getSemester()).getId();
        });
        mvc.perform(get("/api/lecturers/me/classes")
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD))
                        .param("courseClass", "classCode~INT001").param("page", "1")
                        .param("size", "1").param("sort", "classCode,asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(1)).andExpect(jsonPath("$.data.totalPage").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(extraClassId.intValue()));
    }

    @Test
    void legacyLecturerClassListStillAllowsAdminToSeeAllClasses() throws Exception {
        mvc.perform(get("/api/lecturer/classes").header("Authorization", "Bearer " + login("admin", PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(2));
    }

    @Test
    void assignedLecturerCanRecordAttendanceButNotDuplicateIt() throws Exception {
        String token = login("lecturer", PASSWORD);
        String body = attendanceRequest(ownedSession, student);
        mvc.perform(post("/api/lecturer/classes/{id}/attendance", ownedClass.getId()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.studentId").value(student.getId().intValue()))
                .andExpect(jsonPath("$.data.status").value("PRESENT")).andExpect(jsonPath("$.data.method").value(1));
        mvc.perform(post("/api/lecturer/classes/{id}/attendance", ownedClass.getId()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        assertThat(attendances.count()).isEqualTo(1);
    }

    @Test
    void lecturerCannotRecordAttendanceForUnassignedClass() throws Exception {
        mvc.perform(post("/api/lecturer/classes/{id}/attendance", otherClass.getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(attendanceRequest(otherSession, otherStudent)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        assertThat(attendances.count()).isZero();
    }

    @Test
    void cannotSmuggleAnotherClassSessionOrUnenrolledStudentThroughOwnedClass() throws Exception {
        String token = login("lecturer", PASSWORD);
        for (String body : List.of(attendanceRequest(otherSession, student), attendanceRequest(ownedSession, otherStudent))) {
            mvc.perform(post("/api/lecturer/classes/{id}/attendance", ownedClass.getId())
                            .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        }
        assertThat(attendances.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"closed", "expired"})
    void closedOrExpiredSessionCannotAcceptAttendance(String reason) throws Exception {
        if (reason.equals("closed")) {
            ownedSession.setStatus(SessionStatus.CLOSED);
        } else {
            ownedSession.setOpenedAt(LocalDateTime.now().minusHours(2));
            ownedSession.setClosedAt(LocalDateTime.now().minusHours(1));
        }
        sessions.saveAndFlush(ownedSession);
        mvc.perform(post("/api/lecturer/classes/{id}/attendance", ownedClass.getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(attendanceRequest(ownedSession, student)))
                .andExpect(status().isConflict());
        assertThat(attendances.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PRESENT", "LATE", "ABSENT"})
    void assignedLecturerCanRecordManualAttendanceUsingClassSessionId(TimeStatus attendanceStatus) throws Exception {
        String token = login("lecturer", PASSWORD);
        String body = json.writeValueAsString(Map.of("studentId", student.getId(), "status", attendanceStatus,
                "attendanceSessionId", otherSession.getId(), "method", 99, "recognizedAt", "2000-01-01T00:00:00"));
        mvc.perform(post("/api/sessions/{id}/attendance/manual", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.classId").value(ownedClass.getId().intValue()))
                .andExpect(jsonPath("$.data.attendanceSessionId").value(ownedSession.getId().intValue()))
                .andExpect(jsonPath("$.data.studentId").value(student.getId().intValue()))
                .andExpect(jsonPath("$.data.status").value(attendanceStatus.name()))
                .andExpect(jsonPath("$.data.method").value(1));
        Attendance saved = attendances.findAll().getFirst();
        assertThat(saved.getConfidence()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getRecognizedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
        mvc.perform(post("/api/sessions/{id}/attendance/manual", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/lecturer/classes/{id}/attendance", ownedClass.getId())
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(attendanceRequest(ownedSession, student)))
                .andExpect(status().isConflict());
        assertThat(attendances.count()).isEqualTo(1);
    }

    @Test
    void manualAttendanceRequiresActiveEnrollmentInTheSessionClass() throws Exception {
        String token = login("lecturer", PASSWORD);
        transactions.executeWithoutResult(transaction -> entityManager.createQuery(
                "update StudentHasCourseClass e set e.status = :status where e.courseClass.id = :classId")
                .setParameter("status", Status.INACTIVE).setParameter("classId", ownedClass.getId()).executeUpdate());
        for (Long studentId : List.of(student.getId(), otherStudent.getId(), Long.MAX_VALUE)) {
            mvc.perform(post("/api/sessions/{id}/attendance/manual", ownedSession.getClassSession().getId())
                            .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(new ManualAttendanceRequest(studentId, TimeStatus.PRESENT))))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        }
        assertThat(attendances.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"closed", "expired"})
    void newManualEndpointRejectsClosedAndExpiredSessions(String reason) throws Exception {
        if (reason.equals("closed")) {
            ownedSession.setStatus(SessionStatus.CLOSED);
        } else {
            ownedSession.setOpenedAt(LocalDateTime.now().minusHours(2));
            ownedSession.setClosedAt(LocalDateTime.now().minusHours(1));
        }
        sessions.saveAndFlush(ownedSession);
        mvc.perform(post("/api/sessions/{id}/attendance/manual", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ManualAttendanceRequest(student.getId(), TimeStatus.PRESENT))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        assertThat(attendances.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PRESENT", "LATE", "ABSENT"})
    void lecturerCanCorrectAttendanceAfterClosureWithoutChangingOtherFields(TimeStatus attendanceStatus) throws Exception {
        Attendance record = transactions.execute(transaction -> attendance(student, ownedSession));
        Attendance before = attendances.findById(record.getId()).orElseThrow();
        ownedSession.setStatus(SessionStatus.CLOSED);
        sessions.saveAndFlush(ownedSession);
        String token = login("lecturer", PASSWORD);
        mvc.perform(patch("/api/attendance/{id}", record.getId()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "status", attendanceStatus, "studentId", otherStudent.getId(),
                                "attendanceSessionId", otherSession.getId(), "method", 99))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(record.getId().intValue()))
                .andExpect(jsonPath("$.data.status").value(attendanceStatus.name()))
                .andExpect(jsonPath("$.data.studentId").value(student.getId().intValue()))
                .andExpect(jsonPath("$.data.attendanceSessionId").value(ownedSession.getId().intValue()));
        Attendance after = attendances.findById(record.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(attendanceStatus);
        assertThat(after.getRecognizedAt()).isEqualTo(before.getRecognizedAt());
        assertThat(after.getMethod()).isEqualTo(before.getMethod());
        assertThat(after.getConfidence()).isEqualByComparingTo(before.getConfidence());
        assertThat(after.getCreatedAt()).isEqualTo(before.getCreatedAt());
        mvc.perform(patch("/api/attendance/{id}", record.getId()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new UpdateAttendanceRequest(attendanceStatus))))
                .andExpect(status().isOk());
        assertThat(attendances.count()).isEqualTo(1);
        mvc.perform(get("/api/sessions/{id}/attendance/results", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.statistics.recorded").value(1))
                .andExpect(jsonPath("$.data.statistics.present").value(attendanceStatus == TimeStatus.PRESENT ? 1 : 0))
                .andExpect(jsonPath("$.data.statistics.late").value(attendanceStatus == TimeStatus.LATE ? 1 : 0))
                .andExpect(jsonPath("$.data.statistics.absent").value(attendanceStatus == TimeStatus.ABSENT ? 1 : 0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "{broken-json", "{\"status\":null}", "{\"status\":\"ACTIVE\"}",
            "{\"status\":\"INACTIVE\"}", "{\"status\":\"UNKNOWN\"}"})
    void attendanceWritesRejectInvalidBodies(String body) throws Exception {
        Attendance record = transactions.execute(transaction -> attendance(student, ownedSession));
        String token = login("lecturer", PASSWORD);
        mvc.perform(patch("/api/attendance/{id}", record.getId()).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(post("/api/sessions/{id}/attendance/manual", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        assertThat(attendances.findById(record.getId()).orElseThrow().getStatus()).isEqualTo(TimeStatus.PRESENT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"status\":\"PRESENT\"}", "{\"studentId\":null,\"status\":\"PRESENT\"}",
            "{\"studentId\":0,\"status\":\"PRESENT\"}", "{\"studentId\":-1,\"status\":\"PRESENT\"}",
            "{\"studentId\":\"invalid\",\"status\":\"PRESENT\"}"})
    void manualAttendanceRequiresAValidStudentId(String body) throws Exception {
        mvc.perform(post("/api/sessions/{id}/attendance/manual", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        assertThat(attendances.count()).isZero();
    }

    @Test
    void closingAttendanceFillsOnlyMissingActiveStudentsAndIsIdempotent() throws Exception {
        Attendance existing = transactions.execute(transaction -> {
            enroll(ownedClass, otherStudent, Status.ACTIVE);
            enroll(ownedClass, otherStudent, Status.ACTIVE);
            enroll(ownedClass, student, Status.INACTIVE);
            Student inactive = student(user("inactive-enrollment", Role.STUDENT), "SV3", "0900000003");
            enroll(ownedClass, inactive, Status.INACTIVE);
            attendance(otherStudent, otherSession);
            return attendance(student, ownedSession);
        });
        LocalDateTime recognizedAt = attendances.findById(existing.getId()).orElseThrow().getRecognizedAt();
        String token = login("lecturer", PASSWORD);
        mvc.perform(post("/api/sessions/{id}/attendance/close", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(ownedSession.getId().intValue()))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
        AttendanceSession closed = sessions.findById(ownedSession.getId()).orElseThrow();
        assertThat(closed.getClosedAt()).isBefore(ownedSession.getClosedAt());
        var ownedAttendance = attendances.findByAttendanceSession_Id(ownedSession.getId(), PageRequest.of(0, 20));
        assertThat(ownedAttendance.getTotalElements()).isEqualTo(2);
        Attendance absence = ownedAttendance.stream().filter(a -> a.getStudent().getId().equals(otherStudent.getId()))
                .findFirst().orElseThrow();
        assertThat(absence.getStatus()).isEqualTo(TimeStatus.ABSENT);
        assertThat(absence.getRecognizedAt()).isEqualTo(closed.getClosedAt());
        assertThat(absence.getMethod()).isEqualTo(1);
        assertThat(absence.getConfidence()).isEqualByComparingTo(BigDecimal.ZERO);
        Attendance unchanged = attendances.findById(existing.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(TimeStatus.PRESENT);
        assertThat(unchanged.getRecognizedAt()).isEqualTo(recognizedAt);
        assertThat(sessions.findById(otherSession.getId()).orElseThrow().getStatus()).isEqualTo(SessionStatus.OPEN);
        mvc.perform(post("/api/sessions/{id}/attendance/close", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CLOSED"));
        assertThat(sessions.findById(ownedSession.getId()).orElseThrow().getClosedAt()).isEqualTo(closed.getClosedAt());
        assertThat(attendances.count()).isEqualTo(3);
        mvc.perform(get("/api/sessions/{id}/attendance/results", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.statistics.recorded").value(2))
                .andExpect(jsonPath("$.data.statistics.present").value(1))
                .andExpect(jsonPath("$.data.statistics.absent").value(1))
                .andExpect(jsonPath("$.data.statistics.unmarked").value(0));
    }

    @Test
    void expiredSessionCanStillBeFinalizedWithoutExtendingItsDeadline() throws Exception {
        ownedSession.setOpenedAt(LocalDateTime.now().minusHours(2));
        ownedSession.setClosedAt(LocalDateTime.now().minusHours(1));
        sessions.saveAndFlush(ownedSession);
        LocalDateTime deadline = sessions.findById(ownedSession.getId()).orElseThrow().getClosedAt();
        mvc.perform(post("/api/sessions/{id}/attendance/close", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CLOSED"));
        AttendanceSession closed = sessions.findById(ownedSession.getId()).orElseThrow();
        assertThat(closed.getStatus()).isEqualTo(SessionStatus.CLOSED);
        assertThat(closed.getClosedAt()).isEqualTo(deadline);
        assertThat(attendances.findAll()).singleElement().satisfies(absence -> {
            assertThat(absence.getStatus()).isEqualTo(TimeStatus.ABSENT);
            assertThat(absence.getRecognizedAt()).isEqualTo(deadline);
        });
    }

    @Test
    void sessionWithoutActiveEnrollmentsCanBeClosedWithoutCreatingAttendance() throws Exception {
        transactions.executeWithoutResult(transaction -> entityManager.createQuery(
                "update StudentHasCourseClass e set e.status = :status where e.courseClass.id = :classId")
                .setParameter("status", Status.INACTIVE).setParameter("classId", ownedClass.getId()).executeUpdate());
        mvc.perform(post("/api/sessions/{id}/attendance/close", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CLOSED"));
        assertThat(attendances.count()).isZero();
    }

    @Test
    void attendanceResultsArePagedButStatisticsCoverOnlyTheWholeRequestedSession() throws Exception {
        transactions.executeWithoutResult(transaction -> {
            enroll(ownedClass, otherStudent, Status.ACTIVE);
            enroll(ownedClass, otherStudent, Status.ACTIVE);
            Student absent = student(user("absent", Role.STUDENT), "SV3", "0900000003");
            Student unmarked = student(user("unmarked", Role.STUDENT), "SV4", "0900000004");
            Student inactive = student(user("inactive", Role.STUDENT), "SV5", "0900000005");
            enroll(ownedClass, absent, Status.ACTIVE);
            enroll(ownedClass, unmarked, Status.ACTIVE);
            enroll(ownedClass, unmarked, Status.ACTIVE);
            enroll(ownedClass, inactive, Status.INACTIVE);
            attendance(student, ownedSession);
            attendance(otherStudent, ownedSession).setStatus(TimeStatus.LATE);
            attendance(absent, ownedSession).setStatus(TimeStatus.ABSENT);
            attendance(otherStudent, otherSession);
        });
        String token = login("lecturer", PASSWORD);
        mvc.perform(get("/api/sessions/{id}/attendance/results", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + token).param("classId", otherClass.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.session.id").value(ownedSession.getId().intValue()))
                .andExpect(jsonPath("$.data.statistics.recorded").value(3))
                .andExpect(jsonPath("$.data.statistics.present").value(1))
                .andExpect(jsonPath("$.data.statistics.late").value(1))
                .andExpect(jsonPath("$.data.statistics.absent").value(1))
                .andExpect(jsonPath("$.data.statistics.unmarked").value(1))
                .andExpect(jsonPath("$.data.attendance.pageNo").value(0))
                .andExpect(jsonPath("$.data.attendance.pageSize").value(20))
                .andExpect(jsonPath("$.data.attendance.totalPage").value(1))
                .andExpect(jsonPath("$.data.attendance.items[*].studentCode").value(contains("SV1", "SV2", "SV3")))
                .andExpect(jsonPath("$.data.attendance.items[0].studentId").value(student.getId().intValue()))
                .andExpect(jsonPath("$.data.attendance.items[0].fullName").value("SV1"))
                .andExpect(jsonPath("$.data.attendance.items[0].status").value("PRESENT"))
                .andExpect(jsonPath("$.data.attendance.items[0].recognizedAt").exists())
                .andExpect(jsonPath("$.data.attendance.items[0].method").value(1))
                .andExpect(jsonPath("$.data.attendance.items[*].student").doesNotExist())
                .andExpect(jsonPath("$.data.attendance.items[*].password").doesNotExist());
        mvc.perform(get("/api/sessions/{id}/attendance/results", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1").param("size", "1").param("sort", "student.studentCode,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.attendance.pageNo").value(1))
                .andExpect(jsonPath("$.data.attendance.totalPage").value(3))
                .andExpect(jsonPath("$.data.attendance.items.length()").value(1))
                .andExpect(jsonPath("$.data.attendance.items[0].studentCode").value("SV2"))
                .andExpect(jsonPath("$.data.statistics.recorded").value(3))
                .andExpect(jsonPath("$.data.statistics.unmarked").value(1));
        mvc.perform(get("/api/sessions/{id}/attendance/results", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + token).param("page", "10").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.attendance.items").isEmpty())
                .andExpect(jsonPath("$.data.statistics.recorded").value(3));
    }

    @Test
    void emptyAttendanceResultsReportUnmarkedStudentsWithoutCreatingRecords() throws Exception {
        mvc.perform(get("/api/sessions/{id}/attendance/results", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.attendance.items").isEmpty())
                .andExpect(jsonPath("$.data.attendance.totalPage").value(0))
                .andExpect(jsonPath("$.data.statistics.recorded").value(0))
                .andExpect(jsonPath("$.data.statistics.present").value(0))
                .andExpect(jsonPath("$.data.statistics.late").value(0))
                .andExpect(jsonPath("$.data.statistics.absent").value(0))
                .andExpect(jsonPath("$.data.statistics.unmarked").value(1));
        assertThat(attendances.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"notAField", "student.user.password", "student.user.email"})
    void attendanceResultsRejectUnsupportedAndSensitiveSortFields(String field) throws Exception {
        mvc.perform(get("/api/sessions/{id}/attendance/results", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD)).param("sort", field + ",asc"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @ParameterizedTest
    @ValueSource(strings = {"student", "admin", "other-lecturer"})
    void newAttendanceEndpointsRejectWrongRolesAndUnassignedLecturer(String username) throws Exception {
        Attendance record = transactions.execute(transaction -> attendance(student, ownedSession));
        String token = login(username, PASSWORD);
        for (var request : List.of(
                post("/api/sessions/{id}/attendance/manual", ownedSession.getClassSession().getId())
                        .content(json.writeValueAsString(new ManualAttendanceRequest(student.getId(), TimeStatus.PRESENT))),
                patch("/api/attendance/{id}", record.getId()).content("{\"status\":\"ABSENT\"}"),
                post("/api/sessions/{id}/attendance/close", ownedSession.getClassSession().getId()),
                get("/api/sessions/{id}/attendance/results", ownedSession.getClassSession().getId()))) {
            mvc.perform(request.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        }
        assertThat(attendances.findById(record.getId()).orElseThrow().getStatus()).isEqualTo(TimeStatus.PRESENT);
        assertThat(sessions.findById(ownedSession.getId()).orElseThrow().getStatus()).isEqualTo(SessionStatus.OPEN);
        assertThat(attendances.count()).isEqualTo(1);
    }

    @Test
    void newAttendanceEndpointsRequireJwt() throws Exception {
        for (var request : List.of(
                post("/api/sessions/1/attendance/manual").content("{\"studentId\":1,\"status\":\"PRESENT\"}"),
                patch("/api/attendance/1").content("{\"status\":\"LATE\"}"),
                post("/api/sessions/1/attendance/close"), get("/api/sessions/1/attendance/results"))) {
            mvc.perform(request.contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        }
        assertThat(attendances.count()).isZero();
    }

    @Test
    void newAttendanceEndpointsReturnNotFoundForMissingResources() throws Exception {
        String token = login("lecturer", PASSWORD);
        ClassSession unopened = unopenedClassSession(ownedClass);
        for (Long classSessionId : List.of(Long.MAX_VALUE, unopened.getId())) {
            for (var request : List.of(
                    post("/api/sessions/{id}/attendance/manual", classSessionId)
                            .content(json.writeValueAsString(new ManualAttendanceRequest(student.getId(), TimeStatus.PRESENT))),
                    post("/api/sessions/{id}/attendance/close", classSessionId),
                    get("/api/sessions/{id}/attendance/results", classSessionId))) {
                mvc.perform(request.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
            }
        }
        mvc.perform(patch("/api/attendance/{id}", Long.MAX_VALUE).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"LATE\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        assertThat(attendances.count()).isZero();
        assertThat(sessions.count()).isEqualTo(2);
    }

    @Test
    void newAttendanceEndpointsRejectMalformedPathIds() throws Exception {
        String token = login("lecturer", PASSWORD);
        for (var request : List.of(
                post("/api/sessions/not-a-number/attendance/manual").content("{\"studentId\":1,\"status\":\"PRESENT\"}"),
                patch("/api/attendance/not-a-number").content("{\"status\":\"LATE\"}"),
                post("/api/sessions/not-a-number/attendance/close"),
                get("/api/sessions/not-a-number/attendance/results"))) {
            mvc.perform(request.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        }
    }

    @Test
    void recognitionEndpointRequiresMultipartImage() throws Exception {
        mvc.perform(multipart("/api/sessions/{id}/attendance/recognize", ownedSession.getClassSession().getId())
                        .header("Authorization", "Bearer " + login("lecturer", PASSWORD)))
                .andExpect(status().isBadRequest());
        assertThat(attendances.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"manual", "legacy"})
    void concurrentRecordingAndClosingCannotCreateDuplicateOrLateAttendance(String endpoint) throws Exception {
        String token = login("lecturer", PASSWORD);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> record = () -> {
            assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
            var request = endpoint.equals("manual")
                    ? post("/api/sessions/{id}/attendance/manual", ownedSession.getClassSession().getId())
                        .content(json.writeValueAsString(new ManualAttendanceRequest(student.getId(), TimeStatus.PRESENT)))
                    : post("/api/lecturer/classes/{id}/attendance", ownedClass.getId())
                        .content(attendanceRequest(ownedSession, student));
            return mvc.perform(request.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON))
                    .andReturn().getResponse().getStatus();
        };
        Callable<Integer> close = () -> {
            assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
            return mvc.perform(post("/api/sessions/{id}/attendance/close", ownedSession.getClassSession().getId())
                            .header("Authorization", "Bearer " + token)).andReturn().getResponse().getStatus();
        };
        int recordedStatus;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var recorded = executor.submit(record);
            var closed = executor.submit(close);
            start.countDown();
            recordedStatus = recorded.get(20, TimeUnit.SECONDS);
            assertThat(recordedStatus).isIn(201, 409);
            assertThat(closed.get(20, TimeUnit.SECONDS)).isEqualTo(200);
        }
        assertThat(sessions.findById(ownedSession.getId()).orElseThrow().getStatus()).isEqualTo(SessionStatus.CLOSED);
        assertThat(attendances.findAll()).singleElement().extracting(Attendance::getStatus)
                .isEqualTo(recordedStatus == 201 ? TimeStatus.PRESENT : TimeStatus.ABSENT);
    }

    @Test
    void concurrentClosesFinalizeOnceWithoutDuplicateAbsences() throws Exception {
        String token = login("lecturer", PASSWORD);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> close = () -> {
            assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
            return mvc.perform(post("/api/sessions/{id}/attendance/close", ownedSession.getClassSession().getId())
                            .header("Authorization", "Bearer " + token)).andReturn().getResponse().getStatus();
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(close);
            var second = executor.submit(close);
            start.countDown();
            assertThat(first.get(20, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(second.get(20, TimeUnit.SECONDS)).isEqualTo(200);
        }
        assertThat(attendances.findAll()).singleElement().extracting(Attendance::getStatus).isEqualTo(TimeStatus.ABSENT);
    }

    @Test
    void studentCannotWriteAttendanceAndAdminCanManageAnyClass() throws Exception {
        mvc.perform(post("/api/lecturer/classes/{id}/attendance", ownedClass.getId())
                        .header("Authorization", "Bearer " + login("student", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(attendanceRequest(ownedSession, student)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/lecturer/classes/{id}/attendance", otherClass.getId())
                        .header("Authorization", "Bearer " + login("admin", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(attendanceRequest(otherSession, otherStudent)))
                .andExpect(status().isCreated());
    }

    @Test
    void disablingAccountOrChangingRoleInvalidatesExistingToken() throws Exception {
        String studentToken = login("student", PASSWORD);
        studentAccount.setStatus(Status.INACTIVE);
        users.saveAndFlush(studentAccount);
        mvc.perform(get("/api/student/profile").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isUnauthorized());
        String adminToken = login("admin", PASSWORD);
        adminAccount.setRole(Role.STUDENT);
        users.saveAndFlush(adminAccount);
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + login("admin", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenForDeletedAccountIsRejected() throws Exception {
        User ephemeral = transactions.execute(transaction -> user("ephemeral", Role.STUDENT));
        String token = login("ephemeral", PASSWORD);
        users.deleteById(ephemeral.getId());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void ignoresSessionAuthenticationBasicCredentialsAndQueryTokens() throws Exception {
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
        mvc.perform(get("/api/admin/users").session(session)).andExpect(status().isUnauthorized());
        String basic = Base64.getEncoder().encodeToString(("admin:" + PASSWORD).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mvc.perform(get("/api/admin/users").header("Authorization", "Basic " + basic)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").param("access_token", login("student", PASSWORD))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("X-User-Id", adminAccount.getId().toString())).andExpect(status().isUnauthorized());
    }

    @Test
    void corsAllowsOnlyConfiguredFrontendOrigin() throws Exception {
        mvc.perform(options("/api/student/profile").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET").header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
        mvc.perform(options("/api/student/profile").header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void malformedLoginIsBadRequestAndPublicRegistrationIsUnavailable() throws Exception {
        for (String body : List.of("{}", "{broken-json", "{\"username\":\"student\",\"password\":\"\"}")) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.trace").doesNotExist());
        }
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(users.count()).isEqualTo(5);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void serviceLayerPreventsBypassingAdminController() {
        assertThatThrownBy(() -> adminUserService.getUsers(PageRequest.of(0, 20))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> courseClassService.findAllCourseClasses(PageRequest.of(0, 20), null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> classSessionService.getSessions(ownedClass.getId(), PageRequest.of(0, 20)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> classSessionService.createSession(ownedClass.getId(), newSessionRequest()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> attendanceService.openAttendanceSession(ownedSession.getClassSession().getId(),
                new OpenAttendanceSessionRequest(15))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> attendanceService.getAttendanceSession(ownedSession.getClassSession().getId()))
                .isInstanceOf(AccessDeniedException.class);
        assertAttendanceWorkflowRoleDenied();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotBypassLecturerOnlyAttendanceSessionService() {
        assertThatThrownBy(() -> attendanceService.openAttendanceSession(ownedSession.getClassSession().getId(),
                new OpenAttendanceSessionRequest(15))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> attendanceService.getAttendanceSession(ownedSession.getClassSession().getId()))
                .isInstanceOf(AccessDeniedException.class);
        assertAttendanceWorkflowRoleDenied();
    }

    @Test
    void serviceLayerChecksClassOwnershipWithoutController() throws Exception {
        Attendance foreignAttendance = transactions.execute(transaction -> attendance(otherStudent, otherSession));
        var authentication = converter.convert(decoder.decode(login("lecturer", PASSWORD)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            assertThatThrownBy(() -> attendanceService.recordAttendance(otherClass.getId(),
                    new RecordAttendanceRequest(otherSession.getId(), otherStudent.getId(), TimeStatus.PRESENT)))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> courseClassService.getStudentsInCourseClass(otherClass.getId(), PageRequest.of(0, 20), null))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> classSessionService.getSessions(otherClass.getId(), PageRequest.of(0, 20)))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> classSessionService.createSession(otherClass.getId(), newSessionRequest()))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> attendanceService.openAttendanceSession(otherSession.getClassSession().getId(),
                    new OpenAttendanceSessionRequest(15))).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> attendanceService.getAttendanceSession(otherSession.getClassSession().getId()))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> attendanceService.recordManualAttendance(otherSession.getClassSession().getId(),
                    new ManualAttendanceRequest(otherStudent.getId(), TimeStatus.PRESENT)))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> attendanceService.updateAttendance(foreignAttendance.getId(),
                    new UpdateAttendanceRequest(TimeStatus.LATE))).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> attendanceService.closeAttendanceSession(otherSession.getClassSession().getId()))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> attendanceService.getAttendanceResults(otherSession.getClassSession().getId(),
                    PageRequest.of(0, 20))).isInstanceOf(AccessDeniedException.class);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void assertAttendanceWorkflowRoleDenied() {
        Long classSessionId = ownedSession.getClassSession().getId();
        assertThatThrownBy(() -> attendanceService.recordManualAttendance(classSessionId,
                new ManualAttendanceRequest(student.getId(), TimeStatus.PRESENT))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> attendanceService.updateAttendance(Long.MAX_VALUE,
                new UpdateAttendanceRequest(TimeStatus.LATE))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> attendanceService.closeAttendanceSession(classSessionId))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> attendanceService.getAttendanceResults(classSessionId, PageRequest.of(0, 20)))
                .isInstanceOf(AccessDeniedException.class);
    }

    private String login(String username, String password) throws Exception {
        var result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials(username, password)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist()).andExpect(cookie().doesNotExist("JSESSIONID"))
                .andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        return json.readTree(result.getResponse().getContentAsString()).path("data").path("accessToken").asString();
    }

    @Test
    void adminCreationUsesBcryptAndCannotInjectAnAdminRoleIntoStudentDto() throws Exception {
        Map<String, Object> body = Map.of("username", "new-student", "email", "new@example.test", "password", PASSWORD,
                "studentCode", "SVNEW", "fullName", "New Student", "dateOfBirth", "2005-01-01",
                "phone", "0900000009", "role", "ADMIN");
        mvc.perform(post("/api/admin/students").header("Authorization", "Bearer " + login("admin", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isCreated());
        var created = users.findByUsernameIgnoreCaseOrEmailIgnoreCase("new-student", "new-student").getFirst();
        assertThat(created.getRole()).isEqualTo(Role.STUDENT);
        assertThat(created.getPassword()).startsWith("$2");
        assertThat(passwords.matches(PASSWORD, created.getPassword())).isTrue();
    }

    @Test
    void newStudentCannotUseAnotherAccountsEmailAsUsername() throws Exception {
        Map<String, Object> body = Map.of("username", "lecturer@example.test", "email", "new@example.test", "password", PASSWORD,
                "studentCode", "SVNEW", "fullName", "New Student", "dateOfBirth", "2005-01-01", "phone", "0900000009");
        mvc.perform(post("/api/admin/students").header("Authorization", "Bearer " + login("admin", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        assertThat(users.count()).isEqualTo(5);
    }

    @Test
    void validJwtCannotElevateRoleUsingHeadersAndMissingRoutesReturnJson404() throws Exception {
        String token = login("student", PASSWORD);
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token).header("X-Role", "ADMIN"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/does-not-exist").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    private String credentials(String username, String password) {
        return json.writeValueAsString(Map.of("username", username, "password", password));
    }

    @Test
    void publicClassListsCannotSortByRelatedAccountSecrets() throws Exception {
        String token = login("student", PASSWORD);
        mvc.perform(get("/api/students/me/classes").header("Authorization", "Bearer " + token)
                        .param("sort", "lecturer.user.password,asc"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void classRosterCannotSortByStudentAccountSecrets() throws Exception {
        String token = login("lecturer", PASSWORD);
        mvc.perform(get("/api/classes/{id}/students", ownedClass.getId()).header("Authorization", "Bearer " + token)
                        .param("sort", "user.password,asc"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    private JwtClaimsSet.Builder claims() {
        Instant now = Instant.now();
        return JwtClaimsSet.builder().subject(studentAccount.getId().toString()).issuer(jwtProperties.issuer())
                .audience(List.of(jwtProperties.audience())).issuedAt(now).notBefore(now).expiresAt(now.plusSeconds(300))
                .claim("userId", studentAccount.getId()).claim("username", "student").claim("roles", List.of("STUDENT"));
    }

    private String signed(JwtEncoder encoder, JwtClaimsSet claims) {
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private String attendanceRequest(AttendanceSession session, Student student) {
        return json.writeValueAsString(new RecordAttendanceRequest(session.getId(), student.getId(), TimeStatus.PRESENT));
    }

    private User user(String username, Role role) {
        User user = new User(username, username + "@example.test", HASH, role, Status.ACTIVE);
        entityManager.persist(user);
        return user;
    }

    private Student student(User user, String code, String phone) {
        Student student = new Student();
        student.setStudentCode(code);
        student.setFullName(code);
        student.setPhone(phone);
        student.setDateOfBirth(LocalDate.of(2005, 1, 1));
        student.setUser(user);
        entityManager.persist(student);
        return student;
    }

    private Lecturer lecturer(User user, String code) {
        Lecturer lecturer = new Lecturer();
        lecturer.setUser(user);
        lecturer.setLecturerCode(code);
        lecturer.setFullName(code);
        lecturer.setDepartment("CNTT");
        lecturer.setPhone("0900000000");
        entityManager.persist(lecturer);
        return lecturer;
    }

    private CourseClass courseClass(String code, Lecturer lecturer, Subject subject, Semester semester) {
        CourseClass courseClass = new CourseClass();
        courseClass.setClassCode(code);
        courseClass.setLecturer(lecturer);
        courseClass.setSubject(subject);
        courseClass.setSemester(semester);
        courseClass.setMaxStudents(30);
        courseClass.setStatus(Status.ACTIVE);
        entityManager.persist(courseClass);
        return courseClass;
    }

    private CreateClassSessionRequest newSessionRequest() {
        return new CreateClassSessionRequest(LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(11, 0));
    }

    private ClassSession classSession(CourseClass courseClass, int number) {
        ClassSession classSession = new ClassSession();
        classSession.setCourseClass(courseClass);
        classSession.setSessionNo(number);
        classSession.setSessionDate(LocalDate.now());
        classSession.setStartTime(LocalTime.of(8, 0));
        classSession.setEndTime(LocalTime.of(10, 0));
        entityManager.persist(classSession);
        return classSession;
    }

    private ClassSession unopenedClassSession(CourseClass courseClass) {
        return transactions.execute(transaction -> {
            ClassSession session = classSession(courseClass, 3);
            session.setSessionDate(LocalDate.now().plusDays(1));
            return session;
        });
    }

    private AttendanceSession attendanceSession(CourseClass courseClass, int number) {
        AttendanceSession session = new AttendanceSession();
        session.setClassSession(classSession(courseClass, number));
        session.setOpenedAt(LocalDateTime.now());
        session.setClosedAt(LocalDateTime.now().plusHours(1));
        session.setLateAfterMinutes(15);
        session.setStatus(SessionStatus.OPEN);
        entityManager.persist(session);
        return session;
    }

    private void enroll(CourseClass courseClass, Student student, Status status) {
        entityManager.persist(StudentHasCourseClass.builder().courseClass(courseClass).student(student).status(status).build());
    }

    private Attendance attendance(Student student, AttendanceSession session) {
        Attendance attendance = new Attendance();
        attendance.setStudent(student);
        attendance.setAttendanceSession(session);
        attendance.setRecognizedAt(LocalDateTime.now());
        attendance.setStatus(TimeStatus.PRESENT);
        attendance.setMethod(1);
        attendance.setConfidence(BigDecimal.ZERO);
        entityManager.persist(attendance);
        return attendance;
    }
}
