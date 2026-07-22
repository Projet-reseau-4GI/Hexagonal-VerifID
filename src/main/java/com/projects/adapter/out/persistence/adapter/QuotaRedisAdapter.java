package com.projects.adapter.out.persistence.adapter;

import com.projects.application.port.out.QuotaRedisPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Adaptateur Redis pour la gestion du compteur de quota journalier.
 *
 * <p>Clé Redis : {@code quota:{orgId}:{yyyyMMdd}}<br>
 * TTL : 25 heures — couvre le reset minuit UTC avec une marge d'une heure.</p>
 *
 * <p>L'opération INCR est atomique côté Redis, ce qui garantit l'absence de
 * race condition même en environnement multi-instance.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuotaRedisAdapter implements QuotaRedisPort {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Duration TTL = Duration.ofHours(25);

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String buildKey(String orgId, LocalDate date) {
        return "quota:" + orgId + ":" + date.format(DATE_FORMATTER);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Port implementation
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<Long> incrementAndGet(String orgId, LocalDate date) {
        String key = buildKey(orgId, date);
        return reactiveRedisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {
                    // Positionner le TTL uniquement lors de la première incrémentation
                    // pour éviter de repousser l'expiration à chaque appel.
                    if (count == 1L) {
                        return reactiveRedisTemplate.expire(key, TTL)
                                .thenReturn(count);
                    }
                    return Mono.just(count);
                })
                .doOnNext(count -> log.debug("[quota-redis] INCR key={} → {}", key, count))
                .doOnError(e -> log.error("[quota-redis] Erreur INCR key={} : {}", key, e.getMessage()));
    }

    @Override
    public Mono<Long> getCurrentCount(String orgId, LocalDate date) {
        String key = buildKey(orgId, date);
        return reactiveRedisTemplate.opsForValue()
                .get(key)
                .map(val -> {
                    try {
                        return Long.parseLong(val);
                    } catch (NumberFormatException e) {
                        log.warn("[quota-redis] Valeur non numérique pour key={} : {}", key, val);
                        return 0L;
                    }
                })
                .defaultIfEmpty(0L)
                .doOnNext(count -> log.debug("[quota-redis] GET key={} → {}", key, count))
                .doOnError(e -> log.error("[quota-redis] Erreur GET key={} : {}", key, e.getMessage()));
    }

    @Override
    public Mono<Void> deleteKey(String orgId, LocalDate date) {
        String key = buildKey(orgId, date);
        return reactiveRedisTemplate.delete(key)
                .doOnNext(deleted -> log.debug("[quota-redis] DEL key={} → {} clé(s) supprimée(s)", key, deleted))
                .doOnError(e -> log.error("[quota-redis] Erreur DEL key={} : {}", key, e.getMessage()))
                .then();
    }
}
