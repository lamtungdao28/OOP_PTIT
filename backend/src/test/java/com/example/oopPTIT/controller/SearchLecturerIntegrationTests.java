package com.example.oopPTIT.controller;

import com.example.oopPTIT.SecurityTestSupport;
import org.springframework.security.test.context.support.WithMockUser;

import com.example.oopPTIT.entity.Lecturer;
import com.example.oopPTIT.entity.User;
import com.example.oopPTIT.exception.StudentExceptionHandler;
import com.example.oopPTIT.repository.LecturerRepository;
import com.example.oopPTIT.repository.UserRepository;
import com.example.oopPTIT.repository.specification.LecturerSpecificationBuilder;
import com.example.oopPTIT.util.Role;
import com.example.oopPTIT.util.Status;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:search-lecturer;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.open-in-view=false",
        "spring.sql.init.mode=never"
})
@WithMockUser(roles = "ADMIN")
class SearchLecturerIntegrationTests extends SecurityTestSupport {
    @Autowired private AdminLecturerController controller;
    @Autowired private LecturerRepository lecturers;
    @Autowired private UserRepository users;
    private MockMvc mvc;
    private Long middleId;

    @BeforeEach
    void setUp() {
        lecturers.deleteAll();
        users.deleteAll();
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new StudentExceptionHandler()).build();

        saveLecturer("GV001", "Nguyen Van An", "CNTT", "0901234567",
                "an.nguyen", "an@ptit.example", Status.ACTIVE);
        middleId = saveLecturer("GV002", "Tran Thi Binh", "DTVT", "0912345678",
                "binh.tran", "binh@ptit.example", Status.INACTIVE);
        saveLecturer("GV003", "Nguyen Van An", "CNTT", "0987654321",
                "an.other", "an@other.example", Status.ACTIVE);
    }

    @Test
    void returnsStudentStylePageAndIdOrderingWithNoFilters() throws Exception {
        mvc.perform(get("/api/admin/lecturers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.pageNo").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalPage").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].lecturerCode").value("GV001"))
                .andExpect(jsonPath("$.data.items[1].lecturerCode").value("GV002"))
                .andExpect(jsonPath("$.data.items[2].lecturerCode").value("GV003"))
                .andExpect(jsonPath("$.data.items[0].fullName").value("Nguyen Van An"))
                .andExpect(jsonPath("$.data.items[0].department").value("CNTT"))
                .andExpect(jsonPath("$.data.items[0].phone").value("0901234567"))
                .andExpect(jsonPath("$.data.items[0].username").value("an.nguyen"))
                .andExpect(jsonPath("$.data.items[0].email").value("an@ptit.example"))
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.items[0].password").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].user").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].courseClasses").doesNotExist());
    }

    @Test
    void ignoresBlankFilterParameters() throws Exception {
        mvc.perform(get("/api/admin/lecturers").param("lecturer", "", " ", "  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(3));
    }

    @ParameterizedTest
    @CsvSource({
            "fullName, BINH, GV002",
            "lecturerCode, gv003, GV003",
            "department, dtvt, GV002",
            "phone, 0912, GV002",
            "username, BINH.TR, GV002",
            "email, OTHER.EXAMPLE, GV003"
    })
    void searchesEachTextFieldCaseInsensitivelyAndTrimsWhitespace(
            String field, String value, String expectedCode) throws Exception {
        mvc.perform(get("/api/admin/lecturers").param("lecturer", "  " + field + "~  " + value + "  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].lecturerCode").value(expectedCode));
    }

    @Test
    void combinesAllFiltersUsingAnd() throws Exception {
        mvc.perform(get("/api/admin/lecturers").param("lecturer",
                        "fullName~Nguyen", "lecturerCode:GV*", "department:CNTT", "phone~090",
                        "username~AN.NG", "email~PTIT.EXAMPLE", "status:ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].lecturerCode").value("GV001"));
    }

    @ParameterizedTest
    @CsvSource({
            "fullName:nguyen van an, 2", "fullName!Nguyen Van An, 1", "fullName:Nguyen*, 2",
            "fullName:*Binh, 1", "fullName:*Thi*, 1", "fullName:An, 0",
            "status:ACTIVE, 2", "status:INACTIVE, 1", "status:active, 2", "status!ACTIVE, 1",
            "lecturerCode>GV001, 2", "lecturerCode<GV003, 2"
    })
    void supportsStudentOperatorsAndWildcards(String filter, int expectedCount) throws Exception {
        mvc.perform(get("/api/admin/lecturers").param("lecturer", filter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(expectedCount));
    }

    @ParameterizedTest
    @CsvSource({":, 1", "!, 2", ">, 1", "<, 1"})
    void comparesNumericIds(String operator, int expectedCount) throws Exception {
        mvc.perform(get("/api/admin/lecturers").param("lecturer", "id" + operator + middleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(expectedCount));
    }

    @ParameterizedTest
    @ValueSource(strings = {"%", "_", "!"})
    void treatsLikeMetacharactersAsLiteralText(String term) throws Exception {
        saveLecturer("GV_TEST", "Literal%_!Name", "CNTT", "0900000000",
                "literal", "literal@example.test", Status.ACTIVE);

        mvc.perform(get("/api/admin/lecturers").param("lecturer", "fullName~" + term))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].lecturerCode").value("GV_TEST"));
    }

    @Test
    void paginatesAndSortsWithLinkedUserFilter() throws Exception {
        mvc.perform(get("/api/admin/lecturers").param("lecturer", "status:ACTIVE")
                        .param("page", "1").param("size", "1").param("sort", "lecturerCode,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(1))
                .andExpect(jsonPath("$.data.totalPage").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].lecturerCode").value("GV001"));
    }

    @Test
    void supportsMultipleSortFields() throws Exception {
        mvc.perform(get("/api/admin/lecturers").param("sort", "department,asc", "lecturerCode,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].lecturerCode").value("GV003"))
                .andExpect(jsonPath("$.data.items[1].lecturerCode").value("GV001"))
                .andExpect(jsonPath("$.data.items[2].lecturerCode").value("GV002"));
    }

    @Test
    void returnsEmptyItemsForPageBeyondLastPage() throws Exception {
        mvc.perform(get("/api/admin/lecturers").param("page", "3").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(3))
                .andExpect(jsonPath("$.data.totalPage").value(2))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void returnsEmptyItemsWhenFiltersDoNotMatchTogether() throws Exception {
        mvc.perform(get("/api/admin/lecturers").param("lecturer", "department:CNTT", "status:INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPage").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "status:UNKNOWN", "id:abc", "id:9223372036854775808", "unknown:value", "password~secret",
            "user.password~secret", "dateOfBirth:2005-01-01", "fullName=Nguyen", "fullName:",
            "fullName:*", "fullName:**", "id~123", "id:123*", "status~ACTIVE", "status>ACTIVE",
            "status:*ACTIVE*", "invalid-filter"
    })
    void rejectsMalformedOrUnsupportedFilters(String filter) throws Exception {
        mvc.perform(get("/api/admin/lecturers").param("lecturer", filter))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsInvalidFilterEvenWhenOtherFiltersAreValid() throws Exception {
        mvc.perform(get("/api/admin/lecturers").param("lecturer", "fullName~Nguyen", "unknown:value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsUnsupportedSortField() throws Exception {
        mvc.perform(get("/api/admin/lecturers").param("sort", "user.password,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsOverlongFilterValues() throws Exception {
        mvc.perform(get("/api/admin/lecturers").param("lecturer", "fullName~" + "a".repeat(256)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void loadsLinkedUsersWithThePage() {
        var page = lecturers.findAll(new LecturerSpecificationBuilder().build(), PageRequest.of(0, 2));

        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2).allSatisfy(lecturer ->
                assertThat(Persistence.getPersistenceUtil().isLoaded(lecturer, "user")).isTrue());
    }

    private Long saveLecturer(String code, String name, String department, String phone,
                              String username, String email, Status accountStatus) {
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("unused-test-hash");
        user.setRole(Role.LECTURER);
        user.setStatus(accountStatus);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user = users.saveAndFlush(user);

        Lecturer lecturer = new Lecturer();
        lecturer.setLecturerCode(code);
        lecturer.setFullName(name);
        lecturer.setDepartment(department);
        lecturer.setPhone(phone);
        lecturer.setUser(user);
        lecturer.setCreatedAt(now);
        lecturer.setUpdatedAt(now);
        return lecturers.saveAndFlush(lecturer).getId();
    }
}
