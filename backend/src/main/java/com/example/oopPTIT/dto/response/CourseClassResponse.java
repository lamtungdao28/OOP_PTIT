package com.example.oopPTIT.dto.response;

import com.example.oopPTIT.util.Status;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class CourseClassResponse {
    private Long id;
    private String classCode;
    private Integer maxStudents;
    private Status status;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Long semesterId;
    private String semesterName;
    private Long lecturerId;
    private String lecturerCode;
    private String lecturerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
