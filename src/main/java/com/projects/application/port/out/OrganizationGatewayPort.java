package com.projects.application.port.out;

import reactor.core.publisher.Mono;
import java.util.UUID;

/**
 * Port sortant pour la communication avec le module d'organisation (organization_core).
 */
public interface OrganizationGatewayPort {
    Mono<Boolean> isSubscribedToService(UUID tenantId, UUID orgId, String serviceCode, String bearerToken);
    Mono<OrganizationSummaryDTO> getOrganization(UUID tenantId, UUID orgId, String bearerToken);
    
    record OrganizationSummaryDTO(UUID id, UUID tenantId, String name, String code, String status) {}
}
