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
@Table(name = "FACE_PROFILES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FaceProfile extends AbtractEntity<Long> {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "STUDENTSid", nullable = false, unique = true)
    private Student student;

    @Column(name = "embedding", nullable = false)
    private byte[] embedding;

    @Column(name = "model_name", nullable = false, length = 255)
    private String modelName;

    @Column(name = "status", nullable = false, length = 255)
    private String status;
}
