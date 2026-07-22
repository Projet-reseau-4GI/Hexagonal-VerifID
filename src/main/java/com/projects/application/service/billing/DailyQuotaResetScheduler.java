package com.projects.application.service.billing;

import com.projects.application.port.out.OrganizationRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Scheduler qui réinitialise le marqueur de reset quotidien pour chaque organisation,
 * tous les jours à minuit UTC.
 *
 * <p>Note : le compteur Redis expire naturellement après 25 heures (TTL configuré dans
 * {@code QuotaRedisAdapter}). Ce scheduler se contente de mettre à jour la colonne
 * {@code daily_count_reset_at} en base pour assurer la traçabilité et cohérence.</p>
 *
 * <p>Activation : nécessite {@code @EnableScheduling} sur une classe de configuration
 * ou sur la classe principale.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyQuotaResetScheduler {

    private final OrganizationRepositoryPort organizationRepositoryPort;

    /**
     * Se déclenche chaque jour à minuit UTC.
     * Met à jour {@code daily_count_reset_at} pour toutes les organisations actives.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void resetDailyCounters() {
        log.info("[quota-scheduler] Début du reset journalier des compteurs (UTC midnight)");

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        organizationRepositoryPort.findAll()
                .filter(org -> "ACTIVE".equals(org.getStatus()))
                .flatMap(org -> {
                    org.setDailyVerificationCount(0);
                    org.setDailyCountResetAt(now);
                    return organizationRepositoryPort.save(org)
                            .doOnSuccess(saved ->
                                    log.debug("[quota-scheduler] Compteur réinitialisé pour org={}", saved.getId()))
                            .onErrorResume(e -> {
                                log.error("[quota-scheduler] Erreur reset org={} : {}", org.getId(), e.getMessage());
                                return Mono.empty();
                            });
                })
                .count()
                .subscribe(
                        count -> log.info("[quota-scheduler] Reset terminé : {} organisation(s) mises à jour", count),
                        err -> log.error("[quota-scheduler] Erreur globale lors du reset : {}", err.getMessage())
                );
    }
}
