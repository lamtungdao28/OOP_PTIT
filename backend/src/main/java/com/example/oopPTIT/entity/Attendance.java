package com.example.oopPTIT.entity;

import com.example.oopPTIT.util.Status;
import com.example.oopPTIT.util.TimeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ATTENDANCE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Attendance extends AbtractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ATTENDNCE_SESSIONSid", nullable = false)
    private AttendanceSession attendanceSession;

    @Column(name = "recognized_at", nullable = false)
    private LocalDateTime recognizedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 255)
    private TimeStatus status;

    @Column(name = "method", nullable = false)
    private Integer method;

    @Column(name = "confidence", nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "STUDENTSid", nullable = false)
    private Student student;
}
