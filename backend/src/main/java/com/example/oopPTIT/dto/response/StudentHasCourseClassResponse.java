package com.example.oopPTIT.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class StudentHasCourseClassResponse {
    private StudentResponse student;
    private CourseClassResponse courseClass;
}
