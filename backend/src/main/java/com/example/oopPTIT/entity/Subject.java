package com.example.oopPTIT.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Column(name = "status", nullable = false, length = 255)
    private String status;
}
