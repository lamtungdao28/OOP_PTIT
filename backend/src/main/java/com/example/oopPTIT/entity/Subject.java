package com.example.oopPTIT.entity;

import com.example.oopPTIT.util.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "SUBJECTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Subject extends AbtractEntity<Long> {

    @Column(name = "subject_code", nullable = false, unique = true, length = 255)
    private String subjectCode;

    @Column(name = "subject_name", nullable = false, unique = true, length = 255)
    private String subjectName;

    @Column(name = "credits", nullable = false)
    private Integer credits;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 255)
    private Status status;

    @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
    private Set<CourseClass> courseClasses = new HashSet<>();

    @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
    private Set<SemesterHasSubject> semesterHasSubjects = new HashSet<>();
}
