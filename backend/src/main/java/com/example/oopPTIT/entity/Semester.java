package com.example.oopPTIT.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "SEMESTERS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Semester extends AbtractEntity<Long> {

    @Column(name = "name", nullable = false, length = 255)
    private String name;
}
