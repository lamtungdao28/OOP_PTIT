package com.example.oopPTIT.entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.example.oopPTIT.util.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(
            name = "embedding",
            nullable = false,
            length = 8192
    )
    private byte[] embedding;

    @Column(name = "model_name", nullable = false, length = 255)
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 255)
    private Status status;
}
