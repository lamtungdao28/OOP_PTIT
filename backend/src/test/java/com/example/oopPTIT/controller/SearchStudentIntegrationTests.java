package com.example.oopPTIT.controller;

import com.example.oopPTIT.entity.Student;
import com.example.oopPTIT.entity.User;
import com.example.oopPTIT.exception.StudentExceptionHandler;
import com.example.oopPTIT.repository.StudentRepository;
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
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:search-student;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.sql.init.mode=never"
})
class SearchStudentIntegrationTests {
    @Autowired private AdminStudentController controller;
    @Autowired private StudentRepository students;
    @Autowired private UserRepository users;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        students.deleteAll();
        users.deleteAll();
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new StudentExceptionHandler()).build();

        saveStudent("B23DCCN001", "Nguyen Van An", "2005-01-01", "0901234567",
                "an.nguyen", "an@ptit.example", Status.ACTIVE);
        saveStudent("B23DCCN002", "Tran Thi Binh", "2004-06-15", "0912345678",
                "binh.tran", "binh@ptit.example", Status.INACTIVE);
        saveStudent("B24DCCN003", "Nguyen Van An", "2006-12-31", "0987654321",
                "an.other", "an@other.example", Status.ACTIVE);
    }

    @Test
    void returnsSubjectStylePageAndIdOrderingWithNoFilters() throws Exception {
        mvc.perform(get("/api/admin/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.data.pageNo").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalPage").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].studentCode").value("B23DCCN001"))
                .andExpect(jsonPath("$.data.items[1].studentCode").value("B23DCCN002"))
                .andExpect(jsonPath("$.data.items[2].studentCode").value("B24DCCN003"))
                .andExpect(jsonPath("$.data.items[0].user").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].password").doesNotExist());
    }

    @Test
    void keepsNameAndCodeSearchCaseInsensitiveAndTrimmed() throws Exception {
        mvc.perform(get("/api/admin/students").param("student", "  fullName~  nGUYen  ", "studentCode~ b23 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].studentCode").value("B23DCCN001"));
    }

    @Test
    void ignoresBlankTextFilters() throws Exception {
        mvc.perform(get("/api/admin/students").param("student", " ", "", "  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(3));
    }

    @ParameterizedTest
    @CsvSource({
            "fullName, BINH, B23DCCN002",
            "studentCode, b24, B24DCCN003",
            "phone, 0912, B23DCCN002",
            "username, BINH.TR, B23DCCN002",
            "email, OTHER.EXAMPLE, B24DCCN003"
    })
    void searchesEachTextFieldIndependently(String field, String value, String expectedCode) throws Exception {
        mvc.perform(get("/api/admin/students").param("student", field + "~" + value))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].studentCode").value(expectedCode));
    }

    @ParameterizedTest
    @CsvSource({"ACTIVE, 2", "INACTIVE, 1", "active, 2"})
    void filtersStatusOnLinkedUser(String accountStatus, int expectedCount) throws Exception {
        mvc.perform(get("/api/admin/students").param("student", "status:" + accountStatus))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(expectedCount));
    }

    @Test
    void combinesAllFiltersUsingAnd() throws Exception {
        mvc.perform(get("/api/admin/students")
                        .param("student", "fullName~An", "studentCode~B23", "phone~090", "username~AN.NG",
                                "email~PTIT.EXAMPLE", "status:ACTIVE", "dateOfBirth:2005-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].studentCode").value("B23DCCN001"));
    }

    @Test
    void combinesDateComparisonsUsingStrictBoundaries() throws Exception {
        mvc.perform(get("/api/admin/students")
                        .param("student", "dateOfBirth>2004-06-15", "dateOfBirth<2006-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].studentCode").value("B23DCCN001"));
    }

    @ParameterizedTest
    @CsvSource({
            "dateOfBirth>2005-01-01, 1",
            "dateOfBirth<2005-01-01, 1",
            "dateOfBirth:2005-01-01, 1",
            "dateOfBirth!2005-01-01, 2"
    })
    void comparesTypedDates(String filter, int expectedCount) throws Exception {
        mvc.perform(get("/api/admin/students").param("student", filter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(expectedCount));
    }

    @Test
    void returnsEmptyListWhenFiltersDoNotMatchTogether() throws Exception {
        mvc.perform(get("/api/admin/students").param("student", "fullName~Nguyen", "status:INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPage").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"%", "_", "!"})
    void treatsLikeMetacharactersAsLiteralText(String term) throws Exception {
        saveStudent("B23_TEST", "Literal%_!Name", "2005-01-01", "0900000000",
                "literal", "literal@example.test", Status.ACTIVE);

        mvc.perform(get("/api/admin/students").param("student", "fullName~" + term))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].studentCode").value("B23_TEST"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "status:UNKNOWN", "dateOfBirth>not-a-date", "dateOfBirth:2005-02-30", "id:abc",
            "id:9223372036854775808", "unknown:value", "password~secret", "user.password~secret",
            "fullName=Nguyen", "fullName:", "fullName:*", "fullName:**", "status>ACTIVE", "dateOfBirth~2005",
            "id~123", "status:*ACTIVE*", "invalid-filter"
    })
    void rejectsMalformedOrUnsupportedFilters(String filter) throws Exception {
        mvc.perform(get("/api/admin/students").param("student", filter))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"));
    }

    @Test
    void paginatesAndSortsMatchingStudents() throws Exception {
        mvc.perform(get("/api/admin/students")
                        .param("student", "fullName~Nguyen")
                        .param("page", "1").param("size", "1").param("sort", "studentCode,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(1))
                .andExpect(jsonPath("$.data.totalPage").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].studentCode").value("B23DCCN001"));
    }

    @Test
    void returnsEmptyItemsForPageBeyondLastPage() throws Exception {
        mvc.perform(get("/api/admin/students").param("page", "3").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(3))
                .andExpect(jsonPath("$.data.totalPage").value(2))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "fullName:Nguyen Van An, 2", "fullName!nguyen van an, 1", "fullName:Nguyen*, 2",
            "fullName:*Binh, 1", "fullName:*Thi*, 1", "status!ACTIVE, 1", "fullName:An, 0",
            "studentCode>B23DCCN001, 2", "studentCode<B24DCCN003, 2"
    })
    void supportsSubjectOperatorsAndWildcards(String filter, int expectedCount) throws Exception {
        mvc.perform(get("/api/admin/students").param("student", filter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(expectedCount));
    }

    @ParameterizedTest
    @CsvSource({":, 1", "!, 2", ">, 1", "<, 1"})
    void comparesNumericIds(String operator, int expectedCount) throws Exception {
        Long middleId = students.findAll().stream()
                .filter(student -> student.getStudentCode().equals("B23DCCN002"))
                .findFirst().orElseThrow().getId();

        mvc.perform(get("/api/admin/students").param("student", "id" + operator + middleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(expectedCount));
    }

    @Test
    void rejectsUnsupportedSortField() throws Exception {
        mvc.perform(get("/api/admin/students").param("sort", "user.password,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"));
    }

    @Test
    void rejectsOverlongFilterValues() throws Exception {
        mvc.perform(get("/api/admin/students").param("student", "fullName~" + "a".repeat(256)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"));
    }

    private void saveStudent(String code, String name, String birthDate, String phone,
                             String username, String email, Status accountStatus) {
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("unused-test-hash");
        user.setRole(Role.STUDENT);
        user.setStatus(accountStatus);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user = users.saveAndFlush(user);

        Student student = new Student();
        student.setStudentCode(code);
        student.setFullName(name);
        student.setDateOfBirth(LocalDate.parse(birthDate));
        student.setPhone(phone);
        student.setUser(user);
        student.setCreatedAt(now);
        student.setUpdatedAt(now);
        students.saveAndFlush(student);
    }
}
