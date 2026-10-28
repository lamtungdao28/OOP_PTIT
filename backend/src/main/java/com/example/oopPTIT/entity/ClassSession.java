package com.example.oopPTIT.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "CLASS_SESSIONS", uniqueConstraints = @UniqueConstraint(
        name = "uk_class_sessions_class_session_no",
        columnNames = {"COURSE_CLASSESid", "session_no"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassSession extends AbtractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "COURSE_CLASSESid", nullable = false)
    private CourseClass courseClass;

    @Column(name = "session_no", nullable = false)
    private Integer sessionNo;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}
