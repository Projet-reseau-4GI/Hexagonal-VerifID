package com.projects.application.service.billing;

import com.projects.application.port.in.billing.ProcessPaymentUseCase;
import com.projects.application.port.out.OrganizationGatewayPort;
import com.projects.config.ReactiveTenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessPaymentUseCaseImpl implements ProcessPaymentUseCase {

    private final OrganizationGatewayPort organizationGatewayPort;

    @Override
    public Mono<Void> processSuccessfulPayment(String organizationId, String planId, String paymentId) {
        log.info("Processing successful payment {} for org {} with new plan {}", paymentId, organizationId, planId);
        
        return ReactiveTenantContext.getKernelTenantId()
                .defaultIfEmpty(UUID.randomUUID()) // Fallback for webhook without context
                .flatMap(tenantId -> {
                    UUID orgUuid;
                    try {
                        orgUuid = UUID.fromString(organizationId);
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid organizationId format: {}", organizationId);
                        return Mono.error(e);
                    }
                    
                    return organizationGatewayPort.upgradeOrganizationPlan(tenantId, orgUuid, planId)
                            .doOnSuccess(v -> log.info("Successfully upgraded plan for org {}", organizationId));
                });
    }
}
