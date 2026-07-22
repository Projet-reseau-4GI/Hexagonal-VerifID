package com.projects.adapter.out.persistence.adapter;

import com.projects.adapter.out.persistence.entity.OrganizationEntity;
import com.projects.adapter.out.persistence.repository.OrganizationR2dbcRepository;
import com.projects.application.port.out.OrganizationRepositoryPort;
import com.projects.domain.model.Organization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Adaptateur R2DBC pour la persistance locale des organisations VerifID.
 *
 * Effectue le mapping bidirectionnel entre {@link Organization} (domaine)
 * et {@link OrganizationEntity} (table R2DBC).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizationRepositoryAdapter implements OrganizationRepositoryPort {

    private final OrganizationR2dbcRepository repository;
    private final org.springframework.data.r2dbc.core.R2dbcEntityTemplate template;

    @Override
    public Mono<Organization> save(Organization organization) {
        OrganizationEntity entity = toEntity(organization);
        return repository.existsById(entity.getId())
                .flatMap(exists -> {
                    if (exists) {
                        return template.update(entity);
                    } else {
                        return template.insert(entity);
                    }
                })
                .map(this::toDomain)
                .doOnSuccess(o -> log.debug("[org-db] Organisation sauvegardée : id={}", o.getId()))
                .doOnError(e -> log.error("[org-db] Erreur save org id={} : {}", organization.getId(), e.getMessage()));
    }

    @Override
    public Mono<Organization> findById(UUID id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Mono<Organization> findByEmail(String email) {
        return repository.findByEmail(email)
                .map(this::toDomain);
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public Mono<Organization> findByApiKeyHash(String apiKeyHash) {
        return repository.findByApiKeyHash(apiKeyHash)
                .map(this::toDomain);
    }

    @Override
    public Flux<Organization> findAll() {
        return repository.findAll()
                .map(this::toDomain);
    }

    // ── Mappers ─────────────────────────────────────────────────────────────────

    private OrganizationEntity toEntity(Organization org) {
        return OrganizationEntity.builder()
                .id(org.getId())
                .email(org.getEmail())
                .name(org.getName())
                .displayName(org.getDisplayName())
                .logoUri(org.getLogoUri())
                .plan(org.getPlan() != null ? org.getPlan() : "FREE")
                .dailyVerificationCount(org.getDailyVerificationCount() != null ? org.getDailyVerificationCount() : 0)
                .dailyCountResetAt(org.getDailyCountResetAt())
                .createdAt(org.getCreatedAt() != null ? org.getCreatedAt() : LocalDateTime.now())
                .lastSyncedAt(LocalDateTime.now())
                .apiKeyHash(org.getApiKeyHash())
                .apiKeyLabel(org.getApiKeyLabel())
                .apiKeyActive(org.getApiKeyActive())
                .apiKeyCreatedAt(org.getApiKeyCreatedAt())
                .passwordHash(org.getPasswordHash())
                .isEmailVerified(org.getIsEmailVerified())
                .status(org.getStatus() != null ? org.getStatus() : "PENDING")
                .otpCode(org.getOtpCode())
                .otpExpiry(org.getOtpExpiry())
                .clientId(org.getClientId())
                .build();
    }

    private Organization toDomain(OrganizationEntity entity) {
        return Organization.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .name(entity.getName())
                .displayName(entity.getDisplayName())
                .logoUri(entity.getLogoUri())
                .plan(entity.getPlan())
                .dailyVerificationCount(entity.getDailyVerificationCount())
                .dailyCountResetAt(entity.getDailyCountResetAt())
                .createdAt(entity.getCreatedAt())
                .lastSyncedAt(entity.getLastSyncedAt())
                .apiKeyHash(entity.getApiKeyHash())
                .apiKeyLabel(entity.getApiKeyLabel())
                .apiKeyActive(entity.getApiKeyActive())
                .apiKeyCreatedAt(entity.getApiKeyCreatedAt())
                .passwordHash(entity.getPasswordHash())
                .isEmailVerified(entity.getIsEmailVerified())
                .status(entity.getStatus())
                .otpCode(entity.getOtpCode())
                .otpExpiry(entity.getOtpExpiry())
                .clientId(entity.getClientId())
                .build();
    }
}
