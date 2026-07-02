package com.projects.application.service.billing;

import com.projects.application.port.in.billing.CheckQuotaUseCase;
import com.projects.application.port.out.VerificationLogRepositoryPort;
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

    private final com.projects.application.port.out.OrganizationRepositoryPort organizationRepositoryPort;
    private final VerificationLogRepositoryPort verificationLogRepositoryPort;

    private static final int FREEMIUM_LIMIT = 2;

    @Override
    public Mono<Boolean> isQuotaAvailable(String organizationId) {
        UUID orgUuid;
        try {
            orgUuid = UUID.fromString(organizationId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid organizationId format: {}", organizationId);
            return Mono.just(false);
        }
        
        return organizationRepositoryPort.findById(orgUuid)
                .flatMap(org -> {
                    String plan = org.getPlan() != null ? org.getPlan().toUpperCase() : "FREEMIUM";
                    if (!"FREEMIUM".equals(plan)) {
                        // PRO or MAX plans have no limit in this logic
                        return Mono.just(true);
                    }
                    
                    LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
                    return verificationLogRepositoryPort.countByPlatformIdAndDateAfter(orgUuid, startOfDay)
                            .map(count -> {
                                log.debug("Organization {} (FREEMIUM) has {}/{} daily requests", organizationId, count, FREEMIUM_LIMIT);
                                return count < FREEMIUM_LIMIT;
                            });
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("Organization {} not found in local VerifID DB for quota check.", organizationId);
                    return Mono.just(false); // Refuse if not found locally
                }))
                .onErrorResume(e -> {
                    log.error("Error verifying quota for org {}: {}", organizationId, e.getMessage());
                    return Mono.just(false);
                });
    }
}
