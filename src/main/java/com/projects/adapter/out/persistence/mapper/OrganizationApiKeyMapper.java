package com.projects.adapter.out.persistence.mapper;

import com.projects.adapter.out.persistence.entity.OrganizationApiKeyEntity;
import com.projects.domain.model.OrganizationApiKey;

public class OrganizationApiKeyMapper {
    public static OrganizationApiKey toDomain(OrganizationApiKeyEntity entity) {
        if (entity == null) return null;
        return OrganizationApiKey.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .apiKeyHash(entity.getApiKeyHash())
                .label(entity.getLabel())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static OrganizationApiKeyEntity toEntity(OrganizationApiKey domain) {
        if (domain == null) return null;
        return OrganizationApiKeyEntity.builder()
                .id(domain.getId())
                .organizationId(domain.getOrganizationId())
                .apiKeyHash(domain.getApiKeyHash())
                .label(domain.getLabel())
                .active(domain.getActive())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
