package com.example.oopPTIT.entity;

import com.example.oopPTIT.util.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 255)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "LECTURERSid", nullable = false)
    private Lecturer lecturer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SUBJECTSid", nullable = false)
    private Subject subject;

    @OneToMany(mappedBy = "courseClass", fetch = FetchType.LAZY)
    private Set<ClassSession> classSessions = new HashSet<>();

    @OneToMany(mappedBy = "courseClass", fetch = FetchType.LAZY)
    private Set<StudentHasCourseClass> studentHasCourseClasses = new HashSet<>();
}
