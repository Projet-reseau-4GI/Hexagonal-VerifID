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

    private final com.projects.application.port.out.OrganizationRepositoryPort organizationRepositoryPort;

    @Override
    public Mono<Void> processSuccessfulPayment(String organizationId, String planId, String paymentId) {
        log.info("Processing successful payment {} for org {} with new plan {}", paymentId, organizationId, planId);
        
        UUID orgUuid;
        try {
            orgUuid = UUID.fromString(organizationId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid organizationId format: {}", organizationId);
            return Mono.error(e);
        }
        
        return organizationRepositoryPort.findById(orgUuid)
                .switchIfEmpty(Mono.error(new RuntimeException("Organization not found locally to update plan")))
                .flatMap(org -> {
                    org.setPlan(planId); // Update to new plan locally
                    return organizationRepositoryPort.save(org);
                })
                .doOnSuccess(savedOrg -> log.info("Successfully upgraded plan for org {} to {} locally", organizationId, planId))
                .then();
    }
}
