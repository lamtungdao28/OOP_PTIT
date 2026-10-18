package com.example.oopPTIT.controller;

import com.example.oopPTIT.entity.CourseClass;
import com.example.oopPTIT.entity.Lecturer;
import com.example.oopPTIT.entity.Semester;
import com.example.oopPTIT.entity.Subject;
import com.example.oopPTIT.repository.CourseClassRepository;
import com.example.oopPTIT.repository.LecturerRepository;
import com.example.oopPTIT.repository.SemesterRepository;
import com.example.oopPTIT.repository.SubjectRepository;
import com.example.oopPTIT.service.CourseClassService;
import com.example.oopPTIT.util.Status;
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
import org.springframework.dao.DataAccessResourceFailureException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminCourseClassControllerTests {
    private static final String VALID_REQUEST = """
            {"classCode":"INT2204-01","subjectId":10,"semesterId":5,
             "lecturerId":51,"maxStudents":50}
            """;

    private CourseClassRepository courseClassRepository;
    private SubjectRepository subjectRepository;
    private SemesterRepository semesterRepository;
    private LecturerRepository lecturerRepository;
    private MockMvc mockMvc;
    private Subject subject;
    private Semester semester;
    private Lecturer lecturer;
    private CourseClass courseClass;

    @BeforeEach
    void setUp() {
        courseClassRepository = mock(CourseClassRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        semesterRepository = mock(SemesterRepository.class);
        lecturerRepository = mock(LecturerRepository.class);
        var service = new CourseClassService(courseClassRepository, subjectRepository,
                semesterRepository, lecturerRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminCourseClassController(service))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        subject = new Subject();
        subject.setId(10L);
        semester = new Semester();
        semester.setId(5L);
        lecturer = new Lecturer();
        lecturer.setId(51L);
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(subject));
        when(semesterRepository.findById(5L)).thenReturn(Optional.of(semester));
        when(lecturerRepository.findById(51L)).thenReturn(Optional.of(lecturer));

        courseClass = new CourseClass();
        courseClass.setId(12L);
        courseClass.setClassCode("INT2204-01");
        courseClass.setMaxStudents(50);
        courseClass.setStatus(Status.ACTIVE);
        courseClass.setSubject(subject);
        courseClass.setSemester(semester);
        courseClass.setLecturer(lecturer);
        courseClass.setCreatedAt(LocalDateTime.of(2026, 10, 16, 10, 0));
        courseClass.setUpdatedAt(courseClass.getCreatedAt());
        subject.getCourseClasses().add(courseClass);
        lecturer.getCourseClasses().add(courseClass);
    }

    @Test
    void getsClassByPathIdWithoutSerializingEntityRelations() throws Exception {
        when(courseClassRepository.findById(12L)).thenReturn(Optional.of(courseClass));

        mockMvc.perform(get("/api/admin/classes/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(202))
                .andExpect(jsonPath("$.message").value("Get course class detail"))
                .andExpect(jsonPath("$.data.id").value(12))
                .andExpect(jsonPath("$.data.classCode").value("INT2204-01"))
                .andExpect(jsonPath("$.data.maxStudents").value(50))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.subjectId").value(10))
                .andExpect(jsonPath("$.data.semesterId").value(5))
                .andExpect(jsonPath("$.data.lecturerId").value(51))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.subject").doesNotExist())
                .andExpect(jsonPath("$.data.semester").doesNotExist())
                .andExpect(jsonPath("$.data.lecturer").doesNotExist())
                .andExpect(jsonPath("$.data.classSessions").doesNotExist())
                .andExpect(jsonPath("$.data.studentHasCourseClasses").doesNotExist());

        verify(courseClassRepository).findById(12L);
    }

    @Test
    void returns404WhenRequestedClassDoesNotExist() throws Exception {
        when(courseClassRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/classes/99"))
                .andExpect(status().isNotFound());

        verify(courseClassRepository).findById(99L);
    }

    @Test
    void rejectsNonNumericClassIdBeforeAccessingRepository() throws Exception {
        mockMvc.perform(get("/api/admin/classes/invalid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseClassRepository);
    }

    @Test
    void listsClassesWithPaginationAndSortWithoutSerializingEntityRelations() throws Exception {
        var pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "classCode"));
        when(courseClassRepository.findAll(ArgumentMatchers.<Specification<CourseClass>>notNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(courseClass), pageable, 6));

        mockMvc.perform(get("/api/admin/classes")
                        .param("page", "1").param("size", "5").param("sort", "classCode,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(202))
                .andExpect(jsonPath("$.message").value("Get all course class"))
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(5))
                .andExpect(jsonPath("$.data.totalPage").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(12))
                .andExpect(jsonPath("$.data.items[0].classCode").value("INT2204-01"))
                .andExpect(jsonPath("$.data.items[0].maxStudents").value(50))
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.items[0].subjectId").value(10))
                .andExpect(jsonPath("$.data.items[0].semesterId").value(5))
                .andExpect(jsonPath("$.data.items[0].lecturerId").value(51))
                .andExpect(jsonPath("$.data.items[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].subject").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].semester").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].lecturer").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].classSessions").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].studentHasCourseClasses").doesNotExist());
    }

    @Test
    void usesDefaultPaginationWhenNoParametersAreProvided() throws Exception {
        var pageable = PageRequest.of(0, 20);
        when(courseClassRepository.findAll(ArgumentMatchers.<Specification<CourseClass>>notNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(courseClass), pageable, 1));

        mockMvc.perform(get("/api/admin/classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.totalPage").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }

    @Test
    void combinesFiltersUsingTypedNumbersAndStatus() throws Exception {
        var pageable = PageRequest.of(2, 5);
        Root<CourseClass> root = mock(Root.class, RETURNS_DEEP_STUBS);
        Path<String> classCode = root.get("classCode");
        Path<Status> statusPath = root.get("status");
        Path<Integer> maxStudents = root.get("maxStudents");
        Path<Long> semesterId = root.get("semester").get("id");
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Predicate codeFilter = mock(Predicate.class);
        Predicate statusFilter = mock(Predicate.class);
        Predicate sizeFilter = mock(Predicate.class);
        Predicate semesterFilter = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);
        when(classCode.as(String.class)).thenReturn(classCode);
        when(builder.like(classCode, "%INT2204%")).thenReturn(codeFilter);
        when(builder.equal(statusPath, Status.ACTIVE)).thenReturn(statusFilter);
        when(builder.greaterThan(maxStudents, 40)).thenReturn(sizeFilter);
        when(builder.equal(semesterId, 5L)).thenReturn(semesterFilter);
        when(builder.and(codeFilter, statusFilter)).thenReturn(combined);
        when(builder.and(combined, sizeFilter)).thenReturn(combined);
        when(builder.and(combined, semesterFilter)).thenReturn(combined);
        when(courseClassRepository.findAll(ArgumentMatchers.<Specification<CourseClass>>notNull(), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<CourseClass> specification = invocation.getArgument(0);
                    assertThat(specification.toPredicate(root, mock(CriteriaQuery.class), builder)).isSameAs(combined);
                    return new PageImpl<>(List.of(courseClass), pageable, 11);
                });

        mockMvc.perform(get("/api/admin/classes")
                        .param("courseClass", "classCode~INT2204", "status:ACTIVE", "maxStudents>40", "semesterId:5")
                        .param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPage").value(3))
                .andExpect(jsonPath("$.data.items[0].id").value(12));

        verify(builder).like(classCode, "%INT2204%");
        verify(builder).equal(statusPath, Status.ACTIVE);
        verify(builder).greaterThan(maxStudents, 40);
        verify(builder).equal(semesterId, 5L);
    }

    @Test
    void returnsEmptyPageWhenFiltersHaveNoMatches() throws Exception {
        var pageable = PageRequest.of(0, 5);
        when(courseClassRepository.findAll(ArgumentMatchers.<Specification<CourseClass>>notNull(), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        mockMvc.perform(get("/api/admin/classes").param("courseClass", "classCode~Missing").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPage").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void preservesTotalPagesWhenRequestedPageIsPastLastPage() throws Exception {
        var pageable = PageRequest.of(4, 5);
        when(courseClassRepository.findAll(ArgumentMatchers.<Specification<CourseClass>>notNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 11));

        mockMvc.perform(get("/api/admin/classes")
                        .param("courseClass", "classCode~INT2204").param("page", "4").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNo").value(4))
                .andExpect(jsonPath("$.data.totalPage").value(3))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"status:UNKNOWN", "maxStudents>invalid", "semesterId:invalid",
            "missing:value", "status~ACTIVE", "maxStudents~50", "createdAt>not-a-date"})
    void returns400ForUnknownFieldsOrInvalidFilterValues(String filter) throws Exception {
        var pageable = PageRequest.of(0, 20);
        Root<CourseClass> root = mock(Root.class, RETURNS_DEEP_STUBS);
        when(courseClassRepository.findAll(ArgumentMatchers.<Specification<CourseClass>>notNull(), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<CourseClass> specification = invocation.getArgument(0);
                    specification.toPredicate(root, mock(CriteriaQuery.class), mock(CriteriaBuilder.class));
                    return Page.empty(pageable);
                });

        mockMvc.perform(get("/api/admin/classes").param("courseClass", filter))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsActiveClassWithAllReferencesAndReturnsId() throws Exception {
        when(courseClassRepository.save(any(CourseClass.class))).thenAnswer(invocation -> {
            CourseClass courseClass = invocation.getArgument(0);
            courseClass.setId(12L);
            return courseClass;
        });

        mockMvc.perform(post("/api/admin/classes").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("added course class"))
                .andExpect(jsonPath("$.data").value(12));

        var captor = ArgumentCaptor.forClass(CourseClass.class);
        verify(courseClassRepository).save(captor.capture());
        CourseClass saved = captor.getValue();
        assertThat(saved.getClassCode()).isEqualTo("INT2204-01");
        assertThat(saved.getMaxStudents()).isEqualTo(50);
        assertThat(saved.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(saved.getSubject()).isSameAs(subject);
        assertThat(saved.getSemester()).isSameAs(semester);
        assertThat(saved.getLecturer()).isSameAs(lecturer);
    }

    @Test
    void returns404WithoutSavingWhenSubjectDoesNotExist() throws Exception {
        when(subjectRepository.findById(10L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/classes").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isNotFound());

        verifyNoInteractions(courseClassRepository);
    }

    @Test
    void returns404WithoutSavingWhenSemesterDoesNotExist() throws Exception {
        when(semesterRepository.findById(5L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/classes").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isNotFound());

        verifyNoInteractions(courseClassRepository);
    }

    @Test
    void returns404WithoutSavingWhenLecturerDoesNotExist() throws Exception {
        when(lecturerRepository.findById(51L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/classes").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isNotFound());

        verifyNoInteractions(courseClassRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"classCode\":\" \",\"subjectId\":10,\"semesterId\":5,\"lecturerId\":51,\"maxStudents\":50}",
            "{\"classCode\":\"INT2204-01\",\"subjectId\":0,\"semesterId\":5,\"lecturerId\":51,\"maxStudents\":50}",
            "{\"classCode\":\"INT2204-01\",\"subjectId\":10,\"semesterId\":0,\"lecturerId\":51,\"maxStudents\":50}",
            "{\"classCode\":\"INT2204-01\",\"subjectId\":10,\"semesterId\":5,\"lecturerId\":0,\"maxStudents\":50}",
            "{\"classCode\":\"INT2204-01\",\"subjectId\":10,\"semesterId\":5,\"lecturerId\":51,\"maxStudents\":0}",
            "{\"classCode\":\"INT2204-01\",\"subjectId\":10,\"semesterId\":5,\"lecturerId\":51,\"maxStudents\":-1}"
    })
    void rejectsInvalidPayloadBeforeAccessingRepositories(String body) throws Exception {
        mockMvc.perform(post("/api/admin/classes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseClassRepository, subjectRepository, semesterRepository, lecturerRepository);
    }

    @Test
    void returnsErrorResponseWhenSaveFails() throws Exception {
        when(courseClassRepository.save(any(CourseClass.class)))
                .thenThrow(new DataAccessResourceFailureException("Database unavailable"));

        mockMvc.perform(post("/api/admin/classes").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("error to add new course class"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
