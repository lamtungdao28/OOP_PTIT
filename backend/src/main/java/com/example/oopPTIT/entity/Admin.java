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
@Table(name = "ADMINS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Admin extends AbtractEntity<Long> {

    @Column(name = "admin_code", nullable = false, unique = true, length = 255)
    private String adminCode;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USERSid", nullable = false, unique = true)
    private User user;
}
