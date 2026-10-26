package com.example.oopPTIT.controller;

import com.example.oopPTIT.SecurityTestSupport;
import org.springframework.security.test.context.support.WithMockUser;

import com.example.oopPTIT.entity.CourseClass;
import com.example.oopPTIT.entity.Lecturer;
import com.example.oopPTIT.entity.Semester;
import com.example.oopPTIT.entity.Student;
import com.example.oopPTIT.entity.StudentHasCourseClass;
import com.example.oopPTIT.entity.Subject;
import com.example.oopPTIT.entity.User;
import com.example.oopPTIT.exception.StudentExceptionHandler;
import com.example.oopPTIT.repository.CourseClassRepository;
import com.example.oopPTIT.repository.LecturerRepository;
import com.example.oopPTIT.repository.SemesterRepository;
import com.example.oopPTIT.repository.StudentHasCourseClassRepository;
import com.example.oopPTIT.repository.StudentRepository;
import com.example.oopPTIT.repository.SubjectRepository;
import com.example.oopPTIT.repository.UserRepository;
import com.example.oopPTIT.util.Role;
import com.example.oopPTIT.util.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:course-class-students;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.open-in-view=false",
        "spring.sql.init.mode=never"
})
@WithMockUser(roles = "ADMIN")
class CourseClassStudentsIntegrationTests extends SecurityTestSupport {
    @Autowired private CourseClassController controller;
    @Autowired private AdminCourseClassController adminController;
    @Autowired private StudentCourseClassController studentController;
    @Autowired private CourseClassRepository classes;
    @Autowired private StudentHasCourseClassRepository enrollments;
    @Autowired private StudentRepository students;
    @Autowired private LecturerRepository lecturers;
    @Autowired private UserRepository users;
    @Autowired private SubjectRepository subjects;
    @Autowired private SemesterRepository semesters;
    private MockMvc mvc;
    private CourseClass courseClass;
    private CourseClass otherClass;
    private CourseClass emptyClass;
    private Long firstStudentId;
    private Long sharedStudentId;
    private Long otherStudentId;

    @BeforeEach
    void setUp() {
        enrollments.deleteAll();
        classes.deleteAll();
        students.deleteAll();
        lecturers.deleteAll();
        users.deleteAll();
        subjects.deleteAll();
        semesters.deleteAll();
        mvc = MockMvcBuilders.standaloneSetup(controller, adminController, studentController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new StudentExceptionHandler()).build();

        Subject subject = new Subject();
        subject.setSubjectCode("INT2204");
        subject.setSubjectName("Object Oriented Programming");
        subject.setCredits(3);
        subject = subjects.saveAndFlush(subject);
        Semester semester = new Semester();
        semester.setName("2026-1");
        semester = semesters.saveAndFlush(semester);
        Lecturer lecturer = new Lecturer();
        lecturer.setLecturerCode("GV01");
        lecturer.setFullName("Lecturer One");
        lecturer.setDepartment("CNTT");
        lecturer.setPhone("0901234567");
        lecturer.setUser(saveUser("GV01", Role.LECTURER));
        lecturer = lecturers.saveAndFlush(lecturer);

        courseClass = saveClass("INT2204-01", subject, semester, lecturer);
        otherClass = saveClass("INT2204-02", subject, semester, lecturer);
        emptyClass = saveClass("INT2204-03", subject, semester, lecturer);

        Student first = saveStudent("SV002", "Same Name", "0901234562");
        Student second = saveStudent("SV001", "Same Name", "0901234561");
        Student third = saveStudent("SV003", "Another Name", "0901234563");
        Student other = saveStudent("SV999", "Other Class", "0901234569");
        firstStudentId = first.getId();
        sharedStudentId = second.getId();
        otherStudentId = other.getId();
        second.getUser().setStatus(Status.INACTIVE);
        users.saveAndFlush(second.getUser());
        enroll(courseClass, first);
        enroll(courseClass, second);
        enroll(courseClass, third);
        enroll(otherClass, second);
        enroll(otherClass, other);
    }

    @Test
    void myClassesRequiresAuthenticationEvenWhenClientSuppliesIdentity() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(get("/api/students/me/classes"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/students/me/classes")
                        .param("studentId", firstStudentId.toString()).param("username", "SV002")
                        .header("X-Username", "SV002").header("Authorization", "Bearer SV002"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsOnlyAuthenticatedStudentsClassesWithoutExposingEntities() throws Exception {
        mvc.perform(get("/api/students/me/classes").with(studentPrincipal("SV002")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.pageNo").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalPage").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(courseClass.getId().intValue()))
                .andExpect(jsonPath("$.data.items[0].classCode").value("INT2204-01"))
                .andExpect(jsonPath("$.data.items[0].maxStudents").value(50))
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.items[0].subjectId").value(courseClass.getSubject().getId().intValue()))
                .andExpect(jsonPath("$.data.items[0].semesterId").value(courseClass.getSemester().getId().intValue()))
                .andExpect(jsonPath("$.data.items[0].lecturerId").value(courseClass.getLecturer().getId().intValue()))
                .andExpect(jsonPath("$.data.items[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].studentHasCourseClasses").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].classSessions").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].lecturer").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].user").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].password").doesNotExist());
    }

    @Test
    void myClassesIgnoresIdentityOverridesFromQueryParametersAndHeaders() throws Exception {
        mvc.perform(get("/api/students/me/classes").with(studentPrincipal("SV999"))
                        .param("studentId", firstStudentId.toString()).param("username", "SV002")
                        .header("X-Username", "SV002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(otherClass.getId().intValue()));
    }

    @Test
    void paginatesAndSortsMyClassesWithoutDuplicateEnrollmentsInflatingCounts() throws Exception {
        Student first = students.findById(firstStudentId).orElseThrow();
        enroll(courseClass, first);
        enroll(otherClass, first);

        mvc.perform(get("/api/students/me/classes").with(studentPrincipal("SV002"))
                        .param("page", "0").param("size", "1").param("sort", "classCode,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(1))
                .andExpect(jsonPath("$.data.totalPage").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(otherClass.getId().intValue()));
        mvc.perform(get("/api/students/me/classes").with(studentPrincipal("SV002"))
                        .param("page", "1").param("size", "1").param("sort", "classCode,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.totalPage").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(courseClass.getId().intValue()));
        mvc.perform(get("/api/students/me/classes").with(studentPrincipal("SV002"))
                        .param("page", "3").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPage").value(2))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void myClassesCombinesExistingClassFiltersUsingAnd() throws Exception {
        mvc.perform(get("/api/students/me/classes").with(studentPrincipal("SV002"))
                        .param("courseClass", "semesterId:" + courseClass.getSemester().getId(),
                                "subjectId:" + courseClass.getSubject().getId(),
                                "lecturerId:" + courseClass.getLecturer().getId(),
                                "classCode~INT2204", "maxStudents>20", "status:ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(courseClass.getId().intValue()));
        mvc.perform(get("/api/students/me/classes").with(studentPrincipal("SV002"))
                        .param("courseClass", "classCode~INT2204", "status:INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void classFiltersCannotIncludeAnotherStudentsClass() throws Exception {
        mvc.perform(get("/api/students/me/classes").with(studentPrincipal("SV002"))
                        .param("courseClass", "id:" + otherClass.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPage").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void blankClassFiltersDoNotRemoveStudentScope() throws Exception {
        mvc.perform(get("/api/students/me/classes").with(studentPrincipal("SV002"))
                        .param("courseClass", "", " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(courseClass.getId().intValue()));
    }

    @Test
    void returnsEmptyPageForCurrentStudentWithoutEnrollments() throws Exception {
        saveStudent("SVEMPTY", "No Enrollments", "0901234560");

        mvc.perform(get("/api/students/me/classes").with(studentPrincipal("SVEMPTY")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPage").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void myClassesNoLongerIncludesWithdrawnClass() throws Exception {
        enroll(otherClass, students.findById(firstStudentId).orElseThrow());
        mvc.perform(delete("/api/admin/classes/{id}/enrollments/{studentId}", courseClass.getId(), firstStudentId))
                .andExpect(status().isOk());

        mvc.perform(get("/api/students/me/classes").with(studentPrincipal("SV002")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(otherClass.getId().intValue()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "GV01", "SV001"})
    void rejectsUnknownNonStudentOrInactivePrincipals(String username) throws Exception {
        mvc.perform(get("/api/students/me/classes").with(studentPrincipal(username)))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsStudentAccountWithoutStudentProfile() throws Exception {
        saveUser("NOPROFILE", Role.STUDENT);

        mvc.perform(get("/api/students/me/classes").with(studentPrincipal("NOPROFILE")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "LECTURER"})
    void rejectsNonStudentRoleEvenWhenStudentProfileExists(String role) throws Exception {
        User account = users.findAll().stream()
                .filter(user -> user.getUsername().equals("SV002")).findFirst().orElseThrow();
        account.setRole(Role.valueOf(role));
        users.saveAndFlush(account);

        mvc.perform(get("/api/students/me/classes").with(studentPrincipal("SV002")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"id:abc", "semesterId:abc", "status:UNKNOWN", "password~secret", "studentId:1"})
    void rejectsInvalidMyClassFilters(String filter) throws Exception {
        mvc.perform(get("/api/students/me/classes").with(studentPrincipal("SV002")).param("courseClass", filter))
                .andExpect(status().isBadRequest());
    }

    @Test
    void withdrawsOnlyFromRequestedClassWithoutDeletingStudentOrAccount() throws Exception {
        var remainingIds = enrollments.findAll().stream()
                .filter(enrollment -> !enrollment.getCourseClass().getId().equals(otherClass.getId())
                        || !enrollment.getStudent().getId().equals(sharedStudentId))
                .map(StudentHasCourseClass::getId).toList();
        long studentCount = students.count();
        long userCount = users.count();
        long classCount = classes.count();

        mvc.perform(delete("/api/admin/classes/{id}/enrollments/{studentId}", otherClass.getId(), sharedStudentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(enrollments.findAll()).extracting(StudentHasCourseClass::getId)
                .containsExactlyInAnyOrderElementsOf(remainingIds);
        assertThat(students.existsById(sharedStudentId)).isTrue();
        assertThat(students.count()).isEqualTo(studentCount);
        assertThat(users.count()).isEqualTo(userCount);
        assertThat(classes.count()).isEqualTo(classCount);
        mvc.perform(get("/api/classes/{id}/students", otherClass.getId()).param("student", "id:" + sharedStudentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
        mvc.perform(get("/api/classes/{id}/students", courseClass.getId()).param("student", "id:" + sharedStudentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }

    @Test
    void withdrawalRemovesDuplicateEnrollmentsForTheSameClassAndStudent() throws Exception {
        enroll(courseClass, students.findById(firstStudentId).orElseThrow());
        long enrollmentCount = enrollments.count();

        mvc.perform(delete("/api/admin/classes/{id}/enrollments/{studentId}", courseClass.getId(), firstStudentId))
                .andExpect(status().isOk());

        assertThat(enrollments.count()).isEqualTo(enrollmentCount - 2);
        assertThat(students.existsById(firstStudentId)).isTrue();
        mvc.perform(get("/api/classes/{id}/students", courseClass.getId()).param("student", "id:" + firstStudentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void cannotWithdrawStudentWhoIsOnlyEnrolledInAnotherClass() throws Exception {
        var existingIds = enrollments.findAll().stream().map(StudentHasCourseClass::getId).toList();

        mvc.perform(delete("/api/admin/classes/{id}/enrollments/{studentId}", courseClass.getId(), otherStudentId))
                .andExpect(status().isNotFound());

        assertThat(enrollments.findAll()).extracting(StudentHasCourseClass::getId)
                .containsExactlyInAnyOrderElementsOf(existingIds);
    }

    @Test
    void missingClassOrStudentDoesNotDeleteAnyEnrollment() throws Exception {
        var existingIds = enrollments.findAll().stream().map(StudentHasCourseClass::getId).toList();

        mvc.perform(delete("/api/admin/classes/{id}/enrollments/{studentId}", 999999L, firstStudentId))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/admin/classes/{id}/enrollments/{studentId}", courseClass.getId(), 999999L))
                .andExpect(status().isNotFound());

        assertThat(enrollments.findAll()).extracting(StudentHasCourseClass::getId)
                .containsExactlyInAnyOrderElementsOf(existingIds);
    }

    @Test
    void repeatedWithdrawalReturnsNotFoundWithoutDeletingOtherEnrollments() throws Exception {
        mvc.perform(delete("/api/admin/classes/{id}/enrollments/{studentId}", courseClass.getId(), firstStudentId))
                .andExpect(status().isOk());
        var remainingIds = enrollments.findAll().stream().map(StudentHasCourseClass::getId).toList();

        mvc.perform(delete("/api/admin/classes/{id}/enrollments/{studentId}", courseClass.getId(), firstStudentId))
                .andExpect(status().isNotFound());

        assertThat(enrollments.findAll()).extracting(StudentHasCourseClass::getId)
                .containsExactlyInAnyOrderElementsOf(remainingIds);
    }

    @Test
    void returnsOnlyRequestedClassStudentsWithoutExposingEntities() throws Exception {
        mvc.perform(get("/api/classes/{id}/students", courseClass.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.pageNo").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalPage").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].student.studentCode").value("SV002"))
                .andExpect(jsonPath("$.data.items[1].student.studentCode").value("SV001"))
                .andExpect(jsonPath("$.data.items[2].student.studentCode").value("SV003"))
                .andExpect(jsonPath("$.data.items[0].student.fullName").value("Same Name"))
                .andExpect(jsonPath("$.data.items[0].student.dateOfBirth").value("2005-01-01"))
                .andExpect(jsonPath("$.data.items[0].student.phone").value("0901234562"))
                .andExpect(jsonPath("$.data.items[0].courseClass.id").value(courseClass.getId().intValue()))
                .andExpect(jsonPath("$.data.items[0].courseClass.classCode").value("INT2204-01"))
                .andExpect(jsonPath("$.data.items[0].student.user").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].student.password").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].student.studentHasCourseClasses").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].student.attendances").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].courseClass.lecturer").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].courseClass.studentHasCourseClasses").doesNotExist());
    }

    @Test
    void paginatesAndSortsStudentsUsingPageable() throws Exception {
        mvc.perform(get("/api/classes/{id}/students", courseClass.getId())
                        .param("page", "0").param("size", "2").param("sort", "studentCode,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(2))
                .andExpect(jsonPath("$.data.totalPage").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].student.studentCode").value("SV001"))
                .andExpect(jsonPath("$.data.items[1].student.studentCode").value("SV002"));

        mvc.perform(get("/api/classes/{id}/students", courseClass.getId())
                        .param("page", "1").param("size", "2").param("sort", "studentCode,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.totalPage").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].student.studentCode").value("SV003"));
    }

    @Test
    void supportsMultipleStudentSortFields() throws Exception {
        mvc.perform(get("/api/classes/{id}/students", courseClass.getId())
                        .param("sort", "fullName,asc", "studentCode,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].student.studentCode").value("SV003"))
                .andExpect(jsonPath("$.data.items[1].student.studentCode").value("SV002"))
                .andExpect(jsonPath("$.data.items[2].student.studentCode").value("SV001"));
    }

    @Test
    void sortsStudentsDescending() throws Exception {
        mvc.perform(get("/api/classes/{id}/students", courseClass.getId())
                        .param("sort", "studentCode,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].student.studentCode").value("SV003"))
                .andExpect(jsonPath("$.data.items[1].student.studentCode").value("SV002"))
                .andExpect(jsonPath("$.data.items[2].student.studentCode").value("SV001"));
    }

    @ParameterizedTest
    @CsvSource({
            "fullName~another, SV003",
            "studentCode:sv001, SV001",
            "phone~4562, SV002",
            "username:sv002, SV002",
            "email~sv003@example.test, SV003",
            "status:inactive, SV001",
            "studentCode:SV003*, SV003"
    })
    void supportsExistingStudentSpecificationFilters(String filter, String expectedCode) throws Exception {
        mvc.perform(get("/api/classes/{id}/students", courseClass.getId()).param("student", filter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].student.studentCode").value(expectedCode));
    }

    @Test
    void combinesStudentFiltersUsingAnd() throws Exception {
        mvc.perform(get("/api/classes/{id}/students", courseClass.getId())
                        .param("student", "fullName~Same", "dateOfBirth<2006-01-01", "status:ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].student.studentCode").value("SV002"));
    }

    @Test
    void paginatesFilteredStudents() throws Exception {
        mvc.perform(get("/api/classes/{id}/students", courseClass.getId())
                        .param("student", "fullName~Same").param("page", "1").param("size", "1")
                        .param("sort", "studentCode,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(1))
                .andExpect(jsonPath("$.data.totalPage").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].student.studentCode").value("SV002"));
    }

    @Test
    void filtersByStudentId() throws Exception {
        mvc.perform(get("/api/classes/{id}/students", courseClass.getId())
                        .param("student", "id:" + firstStudentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].student.studentCode").value("SV002"));
    }

    @Test
    void filtersCannotIncludeStudentFromAnotherClass() throws Exception {
        mvc.perform(get("/api/classes/{id}/students", courseClass.getId())
                        .param("student", "id:" + otherStudentId, "status:ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPage").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void repeatedEnrollmentDoesNotDuplicateStudentsOrPageCounts() throws Exception {
        enroll(courseClass, students.findById(firstStudentId).orElseThrow());

        mvc.perform(get("/api/classes/{id}/students", courseClass.getId())
                        .param("student", "fullName~Same").param("page", "1").param("size", "1")
                        .param("sort", "studentCode,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPage").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].student.studentCode").value("SV002"));
    }

    @Test
    void ignoresBlankFilters() throws Exception {
        mvc.perform(get("/api/classes/{id}/students", courseClass.getId())
                        .param("student", "", " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(3));
    }

    @ParameterizedTest
    @ValueSource(strings = {"studentCode=SV001", "id:abc", "dateOfBirth:invalid", "status:UNKNOWN",
            "courseClassId:1", "password~secret"})
    void rejectsInvalidStudentFilters(String filter) throws Exception {
        mvc.perform(get("/api/classes/{id}/students", courseClass.getId()).param("student", filter))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void returnsEmptyPageForClassWithoutStudents() throws Exception {
        mvc.perform(get("/api/classes/{id}/students", emptyClass.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPage").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void returnsEmptyItemsForPageBeyondLastPage() throws Exception {
        mvc.perform(get("/api/classes/{id}/students", courseClass.getId())
                        .param("page", "3").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(3))
                .andExpect(jsonPath("$.data.totalPage").value(2))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void returnsNotFoundForMissingClass() throws Exception {
        mvc.perform(get("/api/classes/999999/students"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "1.5", "9223372036854775808"})
    void rejectsMalformedClassIds(String id) throws Exception {
        mvc.perform(get("/api/classes/{id}/students", id))
                .andExpect(status().isBadRequest());
    }

    private User saveUser(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.test");
        user.setPassword("unused-test-hash");
        user.setRole(role);
        user.setStatus(Status.ACTIVE);
        return users.saveAndFlush(user);
    }

    private RequestPostProcessor studentPrincipal(String username) {
        return request -> {
            var authentication = UsernamePasswordAuthenticationToken.authenticated(username, null,
                    List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setUserPrincipal(authentication);
            return request;
        };
    }

    private Student saveStudent(String code, String name, String phone) {
        Student student = new Student();
        student.setStudentCode(code);
        student.setFullName(name);
        student.setDateOfBirth(LocalDate.of(2005, 1, 1));
        student.setPhone(phone);
        student.setUser(saveUser(code, Role.STUDENT));
        return students.saveAndFlush(student);
    }

    private CourseClass saveClass(String code, Subject subject, Semester semester, Lecturer lecturer) {
        CourseClass entity = new CourseClass();
        entity.setClassCode(code);
        entity.setMaxStudents(50);
        entity.setStatus(Status.ACTIVE);
        entity.setSubject(subject);
        entity.setSemester(semester);
        entity.setLecturer(lecturer);
        return classes.saveAndFlush(entity);
    }

    private void enroll(CourseClass courseClass, Student student) {
        enrollments.saveAndFlush(StudentHasCourseClass.builder()
                .courseClass(courseClass)
                .student(student)
                .status(Status.ACTIVE)
                .build());
    }
}
