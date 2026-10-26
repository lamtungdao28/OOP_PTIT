package com.example.oopPTIT.controller;

import com.example.oopPTIT.SecurityTestSupport;
import com.example.oopPTIT.entity.FaceProfile;
import com.example.oopPTIT.entity.Student;
import com.example.oopPTIT.entity.User;
import com.example.oopPTIT.repository.FaceProfileRepository;
import com.example.oopPTIT.repository.StudentRepository;
import com.example.oopPTIT.repository.UserRepository;
import com.example.oopPTIT.security.CurrentUser;
import com.example.oopPTIT.service.FaceAiClient;
import com.example.oopPTIT.util.Role;
import com.example.oopPTIT.util.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:face-profile-tests;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@WithMockUser(roles = "STUDENT")
class StudentFaceProfileIntegrationTests extends SecurityTestSupport {
    @Autowired private StudentFaceProfileController controller;
    @Autowired private FaceProfileRepository profiles;
    @Autowired private StudentRepository students;
    @Autowired private UserRepository users;
    @Autowired private WebApplicationContext webContext;
    @MockitoBean private FaceAiClient aiClient;
    @MockitoBean private CurrentUser currentUser;

    private MockMvc mvc;
    private Long studentId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        profiles.deleteAll();
        students.deleteAll();
        users.deleteAll();
        User user = new User();
        user.setUsername("face-student");
        user.setEmail("face-student@example.test");
        user.setPassword("test-hash");
        user.setRole(Role.STUDENT);
        user.setStatus(Status.ACTIVE);
        user = users.saveAndFlush(user);
        Student student = new Student();
        student.setUser(user);
        student.setStudentCode("FACE001");
        student.setFullName("Face Student");
        student.setDateOfBirth(LocalDate.of(2004, 1, 1));
        student.setPhone("0900000001");
        studentId = students.saveAndFlush(student).getId();
        when(currentUser.id()).thenReturn(user.getId());
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("image", "face.jpg", "image/jpeg", new byte[] {1, 2, 3});
    }

    @Test
    void registrationAndReplacementPersistWithoutExposingEmbedding() throws Exception {
        when(aiClient.register(any())).thenReturn(
                new FaceAiClient.RegistrationResult("SFace-2021dec", Collections.nCopies(128, 0.25f)),
                new FaceAiClient.RegistrationResult("SFace-2021dec", Collections.nCopies(128, 0.5f)));

        mvc.perform(get("/api/students/me/face-profile"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.registered").value(false));

        mvc.perform(multipart("/api/students/me/face-profile").file(image()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.registered").value(true))
                .andExpect(jsonPath("$.data.modelName").value("SFace-2021dec"))
                .andExpect(jsonPath("$.data.embedding").doesNotExist());

        FaceProfile first = profiles.findByStudent_Id(studentId).orElseThrow();
        Long profileId = first.getId();
        assertThat(first.getEmbedding()).hasSize(512);
        assertThat(first.getStatus()).isEqualTo(Status.ACTIVE);

        mvc.perform(multipart("/api/students/me/face-profile").file(image()).with(request -> {
            request.setMethod("PUT");
            return request;
        })).andExpect(status().isOk()).andExpect(jsonPath("$.data.registered").value(true));

        FaceProfile replaced = profiles.findByStudent_Id(studentId).orElseThrow();
        assertThat(replaced.getId()).isEqualTo(profileId);
        assertThat(replaced.getEmbedding()).isNotEqualTo(first.getEmbedding());
        assertThat(profiles.count()).isEqualTo(1);
    }

    @Test
    void rejectsDuplicatePostBeforeCallingAi() throws Exception {
        when(aiClient.register(any())).thenReturn(
                new FaceAiClient.RegistrationResult("SFace-2021dec", Collections.nCopies(128, 0.25f)));
        mvc.perform(multipart("/api/students/me/face-profile").file(image()))
                .andExpect(status().isCreated());
        mvc.perform(multipart("/api/students/me/face-profile").file(image()))
                .andExpect(status().isConflict());
        verify(aiClient, org.mockito.Mockito.times(1)).register(any());
    }

    @Test
    void rejectsInvalidImageWithoutSaving() throws Exception {
        MockMultipartFile text = new MockMultipartFile("image", "face.txt", "text/plain", new byte[] {1});
        mvc.perform(multipart("/api/students/me/face-profile").file(text))
                .andExpect(status().isUnsupportedMediaType());
        assertThat(profiles.count()).isZero();
        verify(aiClient, never()).register(any());
    }

    @Test
    @WithAnonymousUser
    void anonymousUserCannotReadFaceProfile() throws Exception {
        MockMvc securedMvc = MockMvcBuilders.webAppContextSetup(webContext).apply(springSecurity()).build();
        securedMvc.perform(get("/api/students/me/face-profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "LECTURER")
    void lecturerCannotReadStudentFaceProfile() throws Exception {
        MockMvc securedMvc = MockMvcBuilders.webAppContextSetup(webContext).apply(springSecurity()).build();
        securedMvc.perform(get("/api/students/me/face-profile"))
                .andExpect(status().isForbidden());
    }
}
