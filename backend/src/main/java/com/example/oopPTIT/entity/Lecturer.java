package com.example.oopPTIT.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "LECTURERS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lecturer extends AbtractEntity<Long> {

    @Column(name = "lecturer_code", nullable = false, unique = true, length = 255)
    private String lecturerCode;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "department", nullable = false, length = 255)
    private String department;

    @Column(name = "phone", nullable = false, length = 255)
    private String phone;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USERSid", nullable = false, unique = true)
    private User user;
}
