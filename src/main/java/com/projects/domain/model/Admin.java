package com.projects.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Admin {
    private Long id;
    private String email;
    private String passwordHash;
    private String role;
    private String otpCode;
    private LocalDateTime otpExpiry;
    private boolean isVerified;
    private LocalDateTime createdAt;
}
