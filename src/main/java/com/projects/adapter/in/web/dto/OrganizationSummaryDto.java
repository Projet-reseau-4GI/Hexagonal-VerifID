package com.projects.adapter.in.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Summary view of an organization for admin monitoring endpoints.
 */
public record OrganizationSummaryDto(
        UUID id,
        String developerName,
        String organizationName,
        String email,
        String plan,
        long quotaConsumed,
        long quotaLimit,
        boolean apiKeyActive,
        String status,
        LocalDateTime createdAt
) {}
