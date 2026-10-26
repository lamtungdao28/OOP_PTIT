package com.example.oopPTIT.controller;

import com.example.oopPTIT.exception.StudentExceptionHandler;

import com.example.oopPTIT.entity.Semester;
import com.example.oopPTIT.entity.SemesterHasSubject;
import com.example.oopPTIT.repository.SemesterRepository;
import com.example.oopPTIT.service.SemesterService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminSemesterControllerTests {
    private SemesterRepository semesterRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        semesterRepository = mock(SemesterRepository.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AdminSemesterController(new SemesterService(semesterRepository)))
                .setControllerAdvice(new StudentExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void listsSemestersWithPaginationWithoutSerializingSubjectLinks() throws Exception {
        Semester semester = new Semester();
        semester.setId(7L);
        semester.setName("Semester 1 - 2026");
        semester.setCreatedAt(LocalDateTime.of(2026, 10, 14, 10, 0));
        semester.setUpdatedAt(semester.getCreatedAt());
        SemesterHasSubject subjectLink = new SemesterHasSubject();
        subjectLink.setSemester(semester);
        semester.getSemesterHasSubjects().add(subjectLink);
        var pageable = PageRequest.of(1, 5);
        when(semesterRepository.findAll(ArgumentMatchers.<Specification<Semester>>notNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(semester), pageable, 6));

        mockMvc.perform(get("/api/admin/semesters").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(202))
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(5))
                .andExpect(jsonPath("$.data.totalPage").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(7))
                .andExpect(jsonPath("$.data.items[0].name").value("Semester 1 - 2026"))
                .andExpect(jsonPath("$.data.items[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].semesterHasSubjects").doesNotExist());
    }

    @Test
    void combinesNameFiltersAndPassesPaginationAndSortToRepository() throws Exception {
        var pageable = PageRequest.of(2, 5, Sort.by(Sort.Direction.DESC, "name"));
        var root = mock(Root.class);
        Path<String> name = mock(Path.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Predicate contains = mock(Predicate.class);
        Predicate excludes = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);
        when(root.get("name")).thenReturn(name);
        when(builder.like(name, "%2026%")).thenReturn(contains);
        when(builder.notEqual(name, "Semester 2 - 2026")).thenReturn(excludes);
        when(builder.and(contains, excludes)).thenReturn(combined);
        when(semesterRepository.findAll(ArgumentMatchers.<Specification<Semester>>notNull(), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<Semester> specification = invocation.getArgument(0);
                    assertThat(specification.toPredicate(root, mock(CriteriaQuery.class), builder))
                            .isSameAs(combined);
                    return new PageImpl<>(List.of(new Semester()), pageable, 11);
                });

        mockMvc.perform(get("/api/admin/semesters")
                        .param("semester", "name~2026", "name!Semester 2 - 2026")
                        .param("page", "2").param("size", "5").param("sort", "name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(5))
                .andExpect(jsonPath("$.data.totalPage").value(3))
                .andExpect(jsonPath("$.data.items.length()").value(1));

        verify(semesterRepository).findAll(ArgumentMatchers.<Specification<Semester>>notNull(), eq(pageable));
    }

    @Test
    void usesDefaultPaginationAndReturnsEmptyPage() throws Exception {
        var pageable = PageRequest.of(0, 20);
        when(semesterRepository.findAll(ArgumentMatchers.<Specification<Semester>>notNull(), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        mockMvc.perform(get("/api/admin/semesters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalPage").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void preservesTotalPagesWhenRequestedPageIsPastLastPage() throws Exception {
        var pageable = PageRequest.of(4, 5);
        when(semesterRepository.findAll(ArgumentMatchers.<Specification<Semester>>notNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 11));

        mockMvc.perform(get("/api/admin/semesters")
                        .param("semester", "name~2026").param("page", "4").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(4))
                .andExpect(jsonPath("$.data.totalPage").value(3))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void createsSemesterAndReturnsId() throws Exception {
        when(semesterRepository.save(any(Semester.class))).thenAnswer(invocation -> {
            Semester semester = invocation.getArgument(0);
            semester.setId(10L);
            return semester;
        });

        mockMvc.perform(post("/api/admin/semesters").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Semester 1 - 2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data").value(10));

        var captor = ArgumentCaptor.forClass(Semester.class);
        verify(semesterRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Semester 1 - 2026");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":null}", "{\"name\":\"\"}", "{\"name\":\"   \"}"})
    void rejectsMissingOrBlankName(String body) throws Exception {
        mockMvc.perform(post("/api/admin/semesters").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(semesterRepository);
    }

    @Test
    void rejectsNameLongerThan255Characters() throws Exception {
        mockMvc.perform(post("/api/admin/semesters").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "a".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(semesterRepository);
    }

    @Test
    void returnsErrorWhenSaveFails() throws Exception {
        when(semesterRepository.save(any(Semester.class))).thenThrow(new IllegalStateException("Save failed"));
        mockMvc.perform(post("/api/admin/semesters").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Semester 1\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
