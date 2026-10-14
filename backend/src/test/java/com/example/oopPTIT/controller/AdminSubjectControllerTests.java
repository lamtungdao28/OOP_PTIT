package com.example.oopPTIT.controller;

import com.example.oopPTIT.entity.Subject;
import com.example.oopPTIT.repository.SubjectRepository;
import com.example.oopPTIT.service.SubjectService;
import com.example.oopPTIT.util.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminSubjectControllerTests {

    private SubjectRepository subjectRepository;
    private MockMvc mockMvc;
    private Subject subject;

    @BeforeEach
    void setUp() {
        subjectRepository = mock(SubjectRepository.class);
        var controller = new AdminSubjectController(new SubjectService(subjectRepository));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        subject = new Subject();
        subject.setId(7L);
        subject.setSubjectCode("INT2204");
        subject.setSubjectName("Object Oriented Programming");
        subject.setCredits(3);
        subject.setStatus(Status.ACTIVE);
    }

    @Test
    void listsSubjectsAtDocumentedPath() throws Exception {
        var pageable = PageRequest.of(1, 5);
        when(subjectRepository.findAll(ArgumentMatchers.<Specification<Subject>>notNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(subject), pageable, 6));

        mockMvc.perform(get("/api/admin/subjects").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(5))
                .andExpect(jsonPath("$.data.totalPage").value(2))
                .andExpect(jsonPath("$.data.items[0].subjectCode").value("INT2204"));

        verify(subjectRepository).findAll(ArgumentMatchers.<Specification<Subject>>notNull(), eq(pageable));
    }

    @Test
    void paginatesFilteredSubjectsAndReturnsCalculatedTotalPages() throws Exception {
        var pageable = PageRequest.of(2, 5, Sort.by(Sort.Direction.DESC, "subjectName"));
        when(subjectRepository.findAll(ArgumentMatchers.<Specification<Subject>>notNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(subject), pageable, 11));

        mockMvc.perform(get("/api/admin/subjects")
                        .param("subject", "subjectName~Programming")
                        .param("page", "2").param("size", "5").param("sort", "subjectName,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(5))
                .andExpect(jsonPath("$.data.totalPage").value(3))
                .andExpect(jsonPath("$.data.items.length()").value(1));

        verify(subjectRepository).findAll(ArgumentMatchers.<Specification<Subject>>notNull(), eq(pageable));
    }

    @Test
    void returnsZeroTotalPagesWhenFilterHasNoMatches() throws Exception {
        var pageable = PageRequest.of(0, 5);
        when(subjectRepository.findAll(ArgumentMatchers.<Specification<Subject>>notNull(), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        mockMvc.perform(get("/api/admin/subjects")
                        .param("subject", "subjectName~Missing").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPage").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void preservesTotalPagesWhenRequestedPageIsPastTheLastPage() throws Exception {
        var pageable = PageRequest.of(4, 5);
        when(subjectRepository.findAll(ArgumentMatchers.<Specification<Subject>>notNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 11));

        mockMvc.perform(get("/api/admin/subjects")
                        .param("subject", "subjectName~Programming").param("page", "4").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(4))
                .andExpect(jsonPath("$.data.totalPage").value(3))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void createsSubjectAtDocumentedPath() throws Exception {
        when(subjectRepository.save(any(Subject.class))).thenAnswer(invocation -> {
            Subject saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        mockMvc.perform(post("/api/admin/subjects").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectCode":"INT2204","subjectName":"OOP","credits":3}
                                """))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.data").value(10));
    }

    @Test
    void patchesOnlySuppliedFieldUsingPathId() throws Exception {
        when(subjectRepository.findById(7L)).thenReturn(Optional.of(subject));

        mockMvc.perform(patch("/api/admin/subjects/7").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"credits":4}
                                """))
                .andExpect(status().is2xxSuccessful());

        assertThat(subject.getCredits()).isEqualTo(4);
        assertThat(subject.getSubjectCode()).isEqualTo("INT2204");
        assertThat(subject.getSubjectName()).isEqualTo("Object Oriented Programming");
        assertThat(subject.getStatus()).isEqualTo(Status.ACTIVE);
        verify(subjectRepository).save(subject);
    }

    @Test
    void patchesAllSuppliedFields() throws Exception {
        when(subjectRepository.findById(7L)).thenReturn(Optional.of(subject));

        mockMvc.perform(patch("/api/admin/subjects/7").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectCode":"INT2205","subjectName":"Databases",
                                 "credits":4,"status":"INACTIVE"}
                                """))
                .andExpect(status().is2xxSuccessful());

        assertThat(subject.getSubjectCode()).isEqualTo("INT2205");
        assertThat(subject.getSubjectName()).isEqualTo("Databases");
        assertThat(subject.getCredits()).isEqualTo(4);
        assertThat(subject.getStatus()).isEqualTo(Status.INACTIVE);
        verify(subjectRepository).save(subject);
    }

    @Test
    void explicitNullFieldsLeaveSubjectUnchanged() throws Exception {
        when(subjectRepository.findById(7L)).thenReturn(Optional.of(subject));

        mockMvc.perform(patch("/api/admin/subjects/7").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectCode":null,"subjectName":null,"credits":null,"status":null}
                                """))
                .andExpect(status().is2xxSuccessful());

        assertThat(subject.getSubjectCode()).isEqualTo("INT2204");
        assertThat(subject.getSubjectName()).isEqualTo("Object Oriented Programming");
        assertThat(subject.getCredits()).isEqualTo(3);
        assertThat(subject.getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    void returnsHttp404WhenSubjectDoesNotExist() throws Exception {
        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/admin/subjects/99").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"credits":4}
                                """))
                .andExpect(status().isNotFound());

        verify(subjectRepository, never()).save(any(Subject.class));
    }

    @Test
    void rejectsInvalidPatchBeforeUpdatingSubject() throws Exception {
        mockMvc.perform(patch("/api/admin/subjects/7").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"credits":0}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(subjectRepository);
    }
}
