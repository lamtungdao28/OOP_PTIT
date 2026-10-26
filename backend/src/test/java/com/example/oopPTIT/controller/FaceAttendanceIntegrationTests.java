package com.example.oopPTIT.controller;

import com.example.oopPTIT.SecurityTestSupport;
import com.example.oopPTIT.entity.*;
import com.example.oopPTIT.repository.*;
import com.example.oopPTIT.security.CurrentUser;
import com.example.oopPTIT.service.FaceAiClient;
import com.example.oopPTIT.service.FaceEmbeddingCodec;
import com.example.oopPTIT.util.Role;
import com.example.oopPTIT.util.SessionStatus;
import com.example.oopPTIT.util.Status;
import com.example.oopPTIT.util.TimeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:face-attendance-tests;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@WithMockUser(roles = "LECTURER")
class FaceAttendanceIntegrationTests extends SecurityTestSupport {
    @Autowired private LecturerCourseClassController controller;
    @Autowired private AttendanceRepository attendances;
    @Autowired private AttendanceSessionRepository sessions;
    @Autowired private ClassSessionRepository classSessions;
    @Autowired private StudentHasCourseClassRepository enrollments;
    @Autowired private FaceProfileRepository profiles;
    @Autowired private CourseClassRepository classes;
    @Autowired private StudentRepository students;
    @Autowired private LecturerRepository lecturers;
    @Autowired private UserRepository users;
    @Autowired private SubjectRepository subjects;
    @Autowired private SemesterRepository semesters;
    @MockitoBean private FaceAiClient aiClient;
    @MockitoBean private CurrentUser currentUser;

    private MockMvc mvc;
    private AttendanceSession session;
    private Student student;
    private StudentHasCourseClass enrollment;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        attendances.deleteAll();
        sessions.deleteAll();
        classSessions.deleteAll();
        enrollments.deleteAll();
        profiles.deleteAll();
        classes.deleteAll();
        students.deleteAll();
        lecturers.deleteAll();
        users.deleteAll();
        subjects.deleteAll();
        semesters.deleteAll();

        User lecturerUser = user("face-lecturer", Role.LECTURER);
        User studentUser = user("face-student", Role.STUDENT);
        Lecturer lecturer = new Lecturer();
        lecturer.setUser(lecturerUser);
        lecturer.setLecturerCode("FACE-GV");
        lecturer.setFullName("Face Lecturer");
        lecturer.setDepartment("IT");
        lecturer.setPhone("0900000011");
        lecturer = lecturers.saveAndFlush(lecturer);
        student = new Student();
        student.setUser(studentUser);
        student.setStudentCode("FACE-SV");
        student.setFullName("Face Student");
        student.setDateOfBirth(LocalDate.of(2004, 1, 1));
        student.setPhone("0900000012");
        student = students.saveAndFlush(student);
        Subject subject = new Subject();
        subject.setSubjectCode("FACE101");
        subject.setSubjectName("Face Attendance");
        subject.setCredits(3);
        subject.setStatus(Status.ACTIVE);
        subject = subjects.saveAndFlush(subject);
        Semester semester = new Semester();
        semester.setName("Face Test Semester");
        semester = semesters.saveAndFlush(semester);
        CourseClass courseClass = new CourseClass();
        courseClass.setClassCode("FACE101-01");
        courseClass.setMaxStudents(50);
        courseClass.setStatus(Status.ACTIVE);
        courseClass.setLecturer(lecturer);
        courseClass.setSubject(subject);
        courseClass.setSemester(semester);
        courseClass = classes.saveAndFlush(courseClass);
        ClassSession classSession = new ClassSession();
        classSession.setCourseClass(courseClass);
        classSession.setSessionNo(1);
        classSession.setSessionDate(LocalDate.now());
        classSession.setStartTime(LocalTime.of(0, 0));
        classSession.setEndTime(LocalTime.of(23, 59));
        classSession = classSessions.saveAndFlush(classSession);
        session = new AttendanceSession();
        session.setClassSession(classSession);
        session.setOpenedAt(LocalDateTime.now().minusMinutes(5));
        session.setClosedAt(LocalDateTime.now().plusMinutes(30));
        session.setLateAfterMinutes(15);
        session.setStatus(SessionStatus.OPEN);
        session = sessions.saveAndFlush(session);
        enrollment = StudentHasCourseClass.builder().courseClass(courseClass).student(student)
                .status(Status.ACTIVE).build();
        enrollment = enrollments.saveAndFlush(enrollment);
        FaceProfile profile = new FaceProfile();
        profile.setStudent(student);
        profile.setModelName(FaceEmbeddingCodec.MODEL_NAME);
        profile.setStatus(Status.ACTIVE);
        var vector = new ArrayList<>(Collections.nCopies(128, 0.0f));
        vector.set(0, 1.0f);
        profile.setEmbedding(FaceEmbeddingCodec.encode(vector));
        profiles.saveAndFlush(profile);
        when(currentUser.id()).thenReturn(lecturerUser.getId());
    }

    private User user(String name, Role role) {
        User user = new User();
        user.setUsername(name);
        user.setEmail(name + "@example.test");
        user.setPassword("test-hash");
        user.setRole(role);
        user.setStatus(Status.ACTIVE);
        return users.saveAndFlush(user);
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("image", "frame.jpg", "image/jpeg", new byte[] {1, 2, 3});
    }

    private FaceAiClient.RecognitionResult match() {
        return new FaceAiClient.RecognitionResult(student.getId(), 0.8241, FaceEmbeddingCodec.MODEL_NAME);
    }

    @Test
    void recognizesAndRecordsFaceOnlyOnce() throws Exception {
        when(aiClient.recognize(any(), anyList())).thenReturn(match());
        mvc.perform(multipart("/api/sessions/{id}/attendance/recognize", session.getClassSession().getId())
                        .file(image()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.studentId").value(student.getId()))
                .andExpect(jsonPath("$.data.status").value("PRESENT"))
                .andExpect(jsonPath("$.data.method").value(2));
        Attendance saved = attendances.findAll().getFirst();
        assertThat(saved.getConfidence()).isEqualByComparingTo("0.8241");
        mvc.perform(multipart("/api/sessions/{id}/attendance/recognize", session.getClassSession().getId())
                        .file(image()))
                .andExpect(status().isConflict());
        assertThat(attendances.count()).isEqualTo(1);
    }

    @Test
    void rejectsNoMatchWithoutWritingAttendance() throws Exception {
        when(aiClient.recognize(any(), anyList())).thenReturn(
                new FaceAiClient.RecognitionResult(null, 0.2, FaceEmbeddingCodec.MODEL_NAME));
        mvc.perform(multipart("/api/sessions/{id}/attendance/recognize", session.getClassSession().getId())
                        .file(image()))
                .andExpect(status().isUnprocessableEntity());
        assertThat(attendances.count()).isZero();
    }

    @Test
    void refusesClosedSessionBeforeCallingAi() throws Exception {
        session.setStatus(SessionStatus.CLOSED);
        sessions.saveAndFlush(session);
        mvc.perform(multipart("/api/sessions/{id}/attendance/recognize", session.getClassSession().getId())
                        .file(image()))
                .andExpect(status().isConflict());
        verify(aiClient, never()).recognize(any(), anyList());
    }

    @Test
    void rechecksEnrollmentAfterAiReturns() throws Exception {
        when(aiClient.recognize(any(), anyList())).thenAnswer(invocation -> {
            enrollment.setStatus(Status.INACTIVE);
            enrollments.saveAndFlush(enrollment);
            return match();
        });
        mvc.perform(multipart("/api/sessions/{id}/attendance/recognize", session.getClassSession().getId())
                        .file(image()))
                .andExpect(status().isNotFound());
        assertThat(attendances.count()).isZero();
    }

    @Test
    void computesLateStatusFromOpeningTime() throws Exception {
        session.setOpenedAt(LocalDateTime.now().minusMinutes(20));
        session.setLateAfterMinutes(5);
        sessions.saveAndFlush(session);
        when(aiClient.recognize(any(), anyList())).thenReturn(match());
        mvc.perform(multipart("/api/sessions/{id}/attendance/recognize", session.getClassSession().getId())
                        .file(image()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("LATE"));
        assertThat(attendances.findAll().getFirst().getStatus()).isEqualTo(TimeStatus.LATE);
    }

    @Test
    void rechecksSessionAfterAiReturns() throws Exception {
        when(aiClient.recognize(any(), anyList())).thenAnswer(invocation -> {
            session.setStatus(SessionStatus.CLOSED);
            sessions.saveAndFlush(session);
            return match();
        });
        mvc.perform(multipart("/api/sessions/{id}/attendance/recognize", session.getClassSession().getId())
                        .file(image()))
                .andExpect(status().isConflict());
        assertThat(attendances.count()).isZero();
    }

    @Test
    void rejectsAiStudentOutsideCandidateList() throws Exception {
        when(aiClient.recognize(any(), anyList())).thenReturn(
                new FaceAiClient.RecognitionResult(999L, 0.9, FaceEmbeddingCodec.MODEL_NAME));
        mvc.perform(multipart("/api/sessions/{id}/attendance/recognize", session.getClassSession().getId())
                        .file(image()))
                .andExpect(status().isBadGateway());
        assertThat(attendances.count()).isZero();
    }

    @Test
    void refusesClassWithoutActiveFaceCandidates() throws Exception {
        FaceProfile profile = profiles.findByStudent_Id(student.getId()).orElseThrow();
        profile.setStatus(Status.INACTIVE);
        profiles.saveAndFlush(profile);
        mvc.perform(multipart("/api/sessions/{id}/attendance/recognize", session.getClassSession().getId())
                        .file(image()))
                .andExpect(status().isConflict());
        verify(aiClient, never()).recognize(any(), anyList());
    }
}
