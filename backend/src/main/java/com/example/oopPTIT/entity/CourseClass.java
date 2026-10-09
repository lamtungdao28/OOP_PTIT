package com.example.oopPTIT.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "COURSE_CLASSES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseClass extends AbtractEntity<Long> {

    @Column(name = "class_code", nullable = false, length = 255)
    private String classCode;

    @Column(name = "max_students", nullable = false)
    private Integer maxStudents;

    @Column(name = "status", nullable = false, length = 255)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "LECTURERSid", nullable = false)
    private Lecturer lecturer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SUBJECTSid", nullable = false)
    private Subject subject;
}
