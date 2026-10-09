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

import java.time.LocalDateTime;

@Entity
@Table(name = "ATTENDANCE_SESSIONS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSession extends AbtractEntity<Long> {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CLASS_SEESIONSid", nullable = false, unique = true)
    private ClassSession classSession;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at", nullable = false)
    private LocalDateTime closedAt;

    @Column(name = "late_after_minutes")
    private Integer lateAfterMinutes;

    @Column(name = "status", nullable = false, length = 255)
    private String status;
}
