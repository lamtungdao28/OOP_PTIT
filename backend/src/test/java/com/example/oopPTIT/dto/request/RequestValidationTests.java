package com.example.oopPTIT.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestValidationTests {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final LocalDate SESSION_DATE = LocalDate.of(2026, 10, 12);
    private static final LocalTime START_TIME = LocalTime.of(9, 0);
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void acceptsCourseClassExampleJson() {
        var request = JSON.readValue("""
                {"classCode":"INT2204-01","subjectId":10,"semesterId":5,
                 "lecturerId":51,"maxStudents":50}
                """, CreateCourseClassRequest.class);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void acceptsClassSessionExampleJsonWithoutExposingValidationProperty() {
        var request = JSON.readValue("""
                {"sessionDate":"2026-10-12","startTime":"09:00","endTime":"11:00"}
                """, CreateClassSessionRequest.class);

        assertThat(validator.validate(request)).isEmpty();
        assertThat(JSON.writeValueAsString(request)).doesNotContain("timeRangeValid");
    }

    @Test
    void rejectsEqualOrReversedSessionTimes() {
        for (var endTime : List.of(START_TIME, LocalTime.of(8, 0))) {
            var request = new CreateClassSessionRequest(SESSION_DATE, START_TIME, endTime);
            assertInvalid(request, "timeRangeValid");
        }
    }

    @Test
    void requiresBothSessionTimes() {
        assertInvalid(new CreateClassSessionRequest(SESSION_DATE, null, START_TIME), "startTime");
        assertInvalid(new CreateClassSessionRequest(SESSION_DATE, START_TIME, null), "endTime");
    }

    @Test
    void acceptsPartialSubjectAndStudentUpdates() {
        var subject = JSON.readValue("""
                {"credits":3}
                """, UpdateSubjectRequest.class);
        var student = JSON.readValue("""
                {"fullName":"Nguyen Van An"}
                """, UpdateStudentRequest.class);

        assertThat(validator.validate(subject)).isEmpty();
        assertThat(validator.validate(student)).isEmpty();
    }

    @Test
    void rejectsBlankValuesSuppliedInPartialUpdates() {
        for (var blank : List.of("", " ", "\n\t")) {
            var subject = new UpdateSubjectRequest();
            subject.setSubjectName(blank);
            var student = new UpdateStudentRequest();
            student.setFullName(blank);

            assertInvalid(subject, "subjectName");
            assertInvalid(student, "fullName");
        }
    }

    @Test
    void rejectsMissingAndEmptyImages() {
        var empty = new MockMultipartFile("image", "face.jpg", "image/jpeg", new byte[0]);

        assertInvalid(new FaceProfileRequest(), "image");
        assertInvalid(new RecognizeAttendanceRequest(), "image");
        assertInvalid(new FaceProfileRequest(empty), "imageNotEmpty");
        assertInvalid(new RecognizeAttendanceRequest(empty), "imageNotEmpty");
    }

    @Test
    void acceptsNonemptyImageUploads() {
        var image = new MockMultipartFile("image", "face.jpg", "image/jpeg", new byte[]{1, 2, 3});

        assertThat(validator.validate(new FaceProfileRequest(image))).isEmpty();
        assertThat(validator.validate(new RecognizeAttendanceRequest(image))).isEmpty();
    }

    @Test
    void acceptsActiveAndInactiveStatusesFromJson() {
        for (var status : List.of("ACTIVE", "INACTIVE")) {
            var json = """
                    {"status":"%s"}
                    """.formatted(status);
            var manualJson = """
                    {"studentId":1,"status":"%s"}
                    """.formatted(status);

            for (var request : List.of(
                    JSON.readValue(json, UpdateSubjectRequest.class),
                    JSON.readValue(json, UpdateStudentStatusRequest.class),
                    JSON.readValue(json, UpdateAttendanceRequest.class),
                    JSON.readValue(manualJson, ManualAttendanceRequest.class))) {
                assertThat(validator.validate(request)).isEmpty();
            }
        }
    }

    @Test
    void rejectsUnknownAndPreviousAttendanceStatusesFromJson() {
        for (var status : List.of("UNKNOWN", "PRESENT", "LATE", "ABSENT", "active")) {
            var json = """
                    {"status":"%s"}
                    """.formatted(status);

            for (var requestType : List.of(UpdateSubjectRequest.class, UpdateStudentStatusRequest.class,
                    UpdateAttendanceRequest.class, ManualAttendanceRequest.class)) {
                assertThatThrownBy(() -> JSON.readValue(json, requestType))
                        .isInstanceOf(InvalidFormatException.class);
            }
        }
    }

    @Test
    void rejectsNullStatusesForRequestsThatRequireStatus() {
        var json = """
                {"status":null}
                """;
        var manualJson = """
                {"studentId":1,"status":null}
                """;

        for (var request : List.of(
                JSON.readValue(json, UpdateStudentStatusRequest.class),
                JSON.readValue(json, UpdateAttendanceRequest.class),
                JSON.readValue(manualJson, ManualAttendanceRequest.class))) {
            assertInvalid(request, "status");
        }
    }

    @Test
    void acceptsZeroLateMinutesButRejectsNegativeValues() {
        assertThat(validator.validate(new OpenAttendanceSessionRequest(0))).isEmpty();
        assertInvalid(new OpenAttendanceSessionRequest(-1), "lateAfterMinutes");
    }

    private static void assertInvalid(Object request, String property) {
        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains(property);
    }
}
