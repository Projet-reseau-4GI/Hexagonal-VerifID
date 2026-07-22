package com.projects.application.service.billing;

import com.projects.application.port.in.billing.CheckQuotaUseCase;
import com.projects.application.port.out.OrganizationRepositoryPort;
import com.projects.application.port.out.QuotaRedisPort;
import com.projects.domain.model.Plan;
import com.projects.domain.model.QuotaStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Implémentation du quota journalier utilisant Redis comme compteur principal.
 *
 * <p>Règles par plan :</p>
 * <ul>
 *   <li>{@code FREE}    : toujours bloqué (0 vérification/jour)</li>
 *   <li>{@code PREMIUM} : 100 vérifications/jour</li>
 *   <li>{@code MAX}     : 1 000 vérifications/jour</li>
 * </ul>
 *
 * <p>Alertes publiées via Spring {@link ApplicationEventPublisher} :</p>
 * <ul>
 *   <li>90% du quota atteint → log WARN</li>
 *   <li>100% du quota atteint → log ERROR</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckQuotaUseCaseImpl implements CheckQuotaUseCase {

    private final OrganizationRepositoryPort organizationRepositoryPort;
    private final QuotaRedisPort quotaRedisPort;
    private final ApplicationEventPublisher eventPublisher;

    // ─────────────────────────────────────────────────────────────────────────
    // isQuotaAvailable
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<Boolean> isQuotaAvailable(String organizationId) {
        return getQuotaStatus(organizationId)
                .map(status -> !status.exceeded())
                .onErrorResume(e -> {
                    log.error("[quota] Erreur vérification quota org={} : {}", organizationId, e.getMessage());
                    return Mono.just(false);
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getQuotaStatus
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<QuotaStatus> getQuotaStatus(String organizationId) {
        UUID orgUuid = parseUuid(organizationId);
        if (orgUuid == null) {
            return Mono.error(new IllegalArgumentException("Format d'organisationId invalide : " + organizationId));
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant resetAt = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        return organizationRepositoryPort.findById(orgUuid)
                .switchIfEmpty(Mono.error(new RuntimeException("Organisation introuvable : " + organizationId)))
                .flatMap(org -> {
                    Plan plan = Plan.fromString(org.getPlan());

                    // Plan FREE : toujours bloqué, pas besoin de consulter Redis
                    if (plan == Plan.FREE) {
                        return Mono.just(new QuotaStatus(
                                organizationId, plan.name(), 0L, 0L, resetAt, true));
                    }

                    long limit = plan.getDailyLimit();
                    return quotaRedisPort.getCurrentCount(organizationId, today)
                            .map(consumed -> {
                                boolean exceeded = consumed >= limit;
                                return new QuotaStatus(
                                        organizationId, plan.name(), consumed, limit, resetAt, exceeded);
                            });
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // incrementQuota
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<Void> incrementQuota(String organizationId) {
        UUID orgUuid = parseUuid(organizationId);
        if (orgUuid == null) {
            return Mono.error(new IllegalArgumentException("Format d'organisationId invalide : " + organizationId));
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        return organizationRepositoryPort.findById(orgUuid)
                .switchIfEmpty(Mono.error(new RuntimeException("Organisation introuvable : " + organizationId)))
                .flatMap(org -> {
                    Plan plan = Plan.fromString(org.getPlan());
                    if (plan == Plan.FREE) {
                        // Plan FREE : rien à incrémenter
                        return Mono.empty();
                    }
                    return quotaRedisPort.incrementAndGet(organizationId, today)
                            .doOnNext(newCount -> {
                                long limit = plan.getDailyLimit();
                                double usagePercent = (double) newCount / limit * 100.0;
                                if (usagePercent >= 100.0) {
                                    log.error("[quota] Organisation {} ({}) a ATTEINT son quota : {}/{}",
                                            organizationId, plan.name(), newCount, limit);
                                    eventPublisher.publishEvent(new QuotaAlertEvent(
                                            organizationId, plan.name(), newCount, limit, 100));
                                } else if (usagePercent >= 90.0) {
                                    log.warn("[quota] Organisation {} ({}) approche son quota : {}/{} ({:.1f}%)",
                                            organizationId, plan.name(), newCount, limit, usagePercent);
                                    eventPublisher.publishEvent(new QuotaAlertEvent(
                                            organizationId, plan.name(), newCount, limit, 90));
                                }
                            });
                })
                .then();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            log.error("[quota] Format UUID invalide : {}", value);
            return null;
        }
    }
}
