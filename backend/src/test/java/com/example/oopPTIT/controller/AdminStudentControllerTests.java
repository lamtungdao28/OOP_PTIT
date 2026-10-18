package com.example.oopPTIT.controller;

import com.example.oopPTIT.entity.Student;
import com.example.oopPTIT.repository.StudentRepository;
import com.example.oopPTIT.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.oopPTIT.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
        mvc = MockMvcBuilders.standaloneSetup(
                new AdminStudentController(new StudentService(repository,
                        mock(UserRepository.class), mock(PasswordEncoder.class)))).build();
    }

    @Test
    void searchesUsingBothFiltersAndReturnsOnlyStudentDetails() throws Exception {
        var student = new Student();
        student.setId(1L);
        student.setStudentCode("B23DCCN001");
        student.setFullName("Nguyen Van An");
        student.setDateOfBirth(LocalDate.of(2005, 1, 1));
        student.setPhone("0901234567");
        when(repository.findByFullNameContainingIgnoreCaseAndStudentCodeContainingIgnoreCaseOrderByIdAsc(
                "An", "B23")).thenReturn(List.of(student));

        mvc.perform(get("/api/admin/students")
                        .param("fullName", "  An  ").param("studentCode", " B23 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].studentCode").value("B23DCCN001"))
                .andExpect(jsonPath("$.data[0].fullName").value("Nguyen Van An"))
                .andExpect(jsonPath("$.data[0].user").doesNotExist())
                .andExpect(jsonPath("$.data[0].attendances").doesNotExist());
    }

    @Test
    void allowsMissingAndBlankFilters() throws Exception {
        mvc.perform(get("/api/admin/students").param("fullName", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(repository).findByFullNameContainingIgnoreCaseAndStudentCodeContainingIgnoreCaseOrderByIdAsc(
                "", "");
    }

    @Test
    void allowsSearchingByNameOnly() throws Exception {
        mvc.perform(get("/api/admin/students").param("fullName", "An"))
                .andExpect(status().isOk());

        verify(repository).findByFullNameContainingIgnoreCaseAndStudentCodeContainingIgnoreCaseOrderByIdAsc(
                "An", "");
    }

    @Test
    void returnsEmptyListWhenCodeHasNoMatches() throws Exception {
        when(repository.findByFullNameContainingIgnoreCaseAndStudentCodeContainingIgnoreCaseOrderByIdAsc(
                "", "UNKNOWN")).thenReturn(List.of());

        mvc.perform(get("/api/admin/students").param("studentCode", "UNKNOWN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
