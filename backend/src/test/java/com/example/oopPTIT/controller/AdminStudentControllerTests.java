package com.example.oopPTIT.controller;

import com.example.oopPTIT.entity.Student;
import com.example.oopPTIT.exception.StudentExceptionHandler;
import com.example.oopPTIT.repository.StudentRepository;
import com.example.oopPTIT.repository.UserRepository;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.oopPTIT.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminStudentControllerTests {

    private StudentRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository = mock(StudentRepository.class);
        when(repository.findAll(ArgumentMatchers.<Specification<Student>>notNull(), any(Pageable.class)))
                .thenAnswer(invocation -> Page.<Student>empty(invocation.getArgument(1)));
        mvc = MockMvcBuilders.standaloneSetup(
                new AdminStudentController(new StudentService(repository,
                        mock(UserRepository.class), mock(PasswordEncoder.class))))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new StudentExceptionHandler()).build();
    }

    @Test
    void searchesUsingBothFiltersAndReturnsOnlyStudentDetails() throws Exception {
        var student = new Student();
        student.setId(1L);
        student.setStudentCode("B23DCCN001");
        student.setFullName("Nguyen Van An");
        student.setDateOfBirth(LocalDate.of(2005, 1, 1));
        student.setPhone("0901234567");
        Pageable pageable = PageRequest.of(0, 20, Sort.by("id"));
        when(repository.findAll(ArgumentMatchers.<Specification<Student>>notNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(student), pageable, 1));

        mvc.perform(get("/api/admin/students")
                        .param("student", "fullName~An", "studentCode:B23*"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.pageNo").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalPage").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(1))
                .andExpect(jsonPath("$.data.items[0].studentCode").value("B23DCCN001"))
                .andExpect(jsonPath("$.data.items[0].fullName").value("Nguyen Van An"))
                .andExpect(jsonPath("$.data.items[0].user").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].attendances").doesNotExist());
    }

    @Test
    void allowsMissingFiltersWithDefaultPagination() throws Exception {
        mvc.perform(get("/api/admin/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());

        verify(repository).findAll(ArgumentMatchers.<Specification<Student>>notNull(),
                eq(PageRequest.of(0, 20, Sort.by("id"))));
    }

    @Test
    void passesPaginationAndSortToRepository() throws Exception {
        mvc.perform(get("/api/admin/students").param("student", "fullName~An")
                        .param("page", "2").param("size", "5").param("sort", "studentCode,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(5));

        verify(repository).findAll(ArgumentMatchers.<Specification<Student>>notNull(),
                eq(PageRequest.of(2, 5, Sort.by(Sort.Direction.DESC, "studentCode"))));
    }

    @Test
    void returnsEmptyListWhenCodeHasNoMatches() throws Exception {
        mvc.perform(get("/api/admin/students").param("student", "studentCode:UNKNOWN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPage").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void rejectsInvalidFiltersBeforeQueryingRepository() throws Exception {
        mvc.perform(get("/api/admin/students").param("student", "password~secret"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(repository);
    }
}
