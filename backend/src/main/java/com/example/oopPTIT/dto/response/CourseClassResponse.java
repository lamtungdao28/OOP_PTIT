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
    private Long semesterId;
    private Long lecturerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
