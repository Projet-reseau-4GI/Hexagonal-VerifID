package com.projects.application.service.billing;

import com.projects.application.port.in.billing.CheckQuotaUseCase;
import com.projects.application.port.out.OrganizationGatewayPort;
import com.projects.application.port.out.VerificationLogRepositoryPort;
import com.projects.config.ReactiveTenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckQuotaUseCaseImpl implements CheckQuotaUseCase {

    private final OrganizationGatewayPort organizationGatewayPort;
    private final VerificationLogRepositoryPort verificationLogRepositoryPort;

    private static final int FREEMIUM_LIMIT = 20;

    @Override
    public Mono<Boolean> isQuotaAvailable(String organizationId) {
        return ReactiveTenantContext.getKernelTenantId()
                .defaultIfEmpty(UUID.randomUUID()) // If missing, we still try
                .flatMap(tenantId -> {
                    UUID orgUuid;
                    try {
                        orgUuid = UUID.fromString(organizationId);
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid organizationId format: {}", organizationId);
                        return Mono.just(false);
                    }
                    
                    return organizationGatewayPort.getOrganization(tenantId, orgUuid, null)
                            .flatMap(org -> {
                                String plan = org.plan() != null ? org.plan().toUpperCase() : "FREEMIUM";
                                if (!"FREEMIUM".equals(plan)) {
                                    // PRO or MAX plans have no limit in this logic
                                    return Mono.just(true);
                                }
                                
                                LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
                                return verificationLogRepositoryPort.countByPlatformIdAndDateAfter(organizationId, startOfDay)
                                        .map(count -> {
                                            log.debug("Organization {} (FREEMIUM) has {}/{} daily requests", organizationId, count, FREEMIUM_LIMIT);
                                            return count < FREEMIUM_LIMIT;
                                        });
                            })
                            .onErrorResume(e -> {
                                log.error("Error verifying quota for org {}: {}", organizationId, e.getMessage());
                                // By default if organization core is unreachable, we could reject or allow. 
                                // Let's reject to be safe, or allow to be resilient. Rejecting is safer for billing.
                                return Mono.just(false);
                            });
                });
    }
}
