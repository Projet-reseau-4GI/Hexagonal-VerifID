package com.projects.adapter.out.persistence.mapper;

import com.projects.adapter.out.persistence.entity.AdminEntity;
import com.projects.domain.model.Admin;
import org.springframework.stereotype.Component;

@Component
public class AdminMapper {

    public Admin toDomain(AdminEntity entity) {
        if (entity == null) {
            return null;
        }
        return Admin.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .role(entity.getRole())
                .otpCode(entity.getOtpCode())
                .otpExpiry(entity.getOtpExpiry())
                .isVerified(entity.isVerified())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public AdminEntity toEntity(Admin domain) {
        if (domain == null) {
            return null;
        }
        return AdminEntity.builder()
                .id(domain.getId())
                .email(domain.getEmail())
                .passwordHash(domain.getPasswordHash())
                .role(domain.getRole())
                .otpCode(domain.getOtpCode())
                .otpExpiry(domain.getOtpExpiry())
                .isVerified(domain.isVerified())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
