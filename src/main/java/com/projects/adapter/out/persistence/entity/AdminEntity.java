package com.projects.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("admins")
public class AdminEntity {
    @Id
    private Long id;
    private String name;
    private String email;
    private String passwordHash;
    private String role;
    private String otpCode;
    private LocalDateTime otpExpiry;
    private boolean isVerified;
    private LocalDateTime createdAt;
}
