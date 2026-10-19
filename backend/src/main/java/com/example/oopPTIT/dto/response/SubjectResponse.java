package com.example.oopPTIT.dto.response;

import com.example.oopPTIT.util.Status;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class SubjectResponse {
    private Long id;
    private String subjectCode;
    private String subjectName;
    private Integer credits;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
