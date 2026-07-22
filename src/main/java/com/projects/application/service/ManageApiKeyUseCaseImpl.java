package com.projects.application.service;

import com.projects.adapter.in.web.dto.ApiKeyResponse;
import com.projects.adapter.out.security.SecurityUtils;
import com.projects.application.port.in.ManageApiKeyUseCase;
import com.projects.application.port.out.OrganizationRepositoryPort;
import com.projects.application.port.out.EmailServicePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageApiKeyUseCaseImpl implements ManageApiKeyUseCase {

    private final OrganizationRepositoryPort organizationRepository;
    private final EmailServicePort emailServicePort;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public Mono<ApiKeyResponse> generateApiKey(UUID organizationId, String label) {
        return organizationRepository.findById(organizationId)
                .switchIfEmpty(Mono.error(new RuntimeException("Organisation introuvable")))
                .flatMap(org -> {
                    byte[] randomBytes = new byte[32];
                    secureRandom.nextBytes(randomBytes);
                    String rawApiKey = "vf_id_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
                    String hashedKey = SecurityUtils.hashApiKey(rawApiKey);

                    org.setApiKeyHash(hashedKey);
                    org.setApiKeyLabel(label != null && !label.isBlank() ? label : "Default Key");
                    org.setApiKeyActive(true);
                    org.setApiKeyCreatedAt(LocalDateTime.now());

                    return organizationRepository.save(org)
                            .flatMap(savedOrg -> emailServicePort
                                    .sendApiKeyCreatedNotification(savedOrg.getEmail(), savedOrg.getDisplayName())
                                    .thenReturn(savedOrg))
                            .map(savedOrg -> new ApiKeyResponse(
                                    savedOrg.getApiKeyLabel(),
                                    rawApiKey, // Retournée EN CLAIR une seule fois
                                    savedOrg.getApiKeyActive(),
                                    savedOrg.getApiKeyCreatedAt()
                            ));
                });
    }

    @Override
    public Mono<ApiKeyResponse> rotateApiKey(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .switchIfEmpty(Mono.error(new RuntimeException("Organisation introuvable")))
                .flatMap(org -> {
                    // Invalider l'ancienne clé dans Redis avant de la remplacer
                    String oldHash = org.getApiKeyHash();
                    Mono<Boolean> evictOld = (oldHash != null)
                            ? redisTemplate.delete("apikey:" + oldHash).thenReturn(true)
                            : Mono.just(true);

                    return evictOld.flatMap(ignored -> {
                        byte[] randomBytes = new byte[32];
                        secureRandom.nextBytes(randomBytes);
                        String newRawKey = "vf_id_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
                        String newHash = SecurityUtils.hashApiKey(newRawKey);

                        org.setApiKeyHash(newHash);
                        org.setApiKeyLabel(org.getApiKeyLabel() != null ? org.getApiKeyLabel() : "Default Key");
                        org.setApiKeyActive(true);
                        org.setApiKeyCreatedAt(LocalDateTime.now());

                        return organizationRepository.save(org)
                                .map(savedOrg -> {
                                    log.info("[apikey] Rotation effectuée pour org={}", organizationId);
                                    return new ApiKeyResponse(
                                            savedOrg.getApiKeyLabel(),
                                            newRawKey,
                                            true,
                                            savedOrg.getApiKeyCreatedAt()
                                    );
                                });
                    });
                });
    }

    @Override
    public Mono<Void> revokeApiKey(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .switchIfEmpty(Mono.error(new RuntimeException("Organisation introuvable")))
                .flatMap(org -> {
                    String hash = org.getApiKeyHash();
                    org.setApiKeyActive(false);

                    Mono<Boolean> evict = (hash != null)
                            ? redisTemplate.delete("apikey:" + hash).thenReturn(true)
                            : Mono.just(true);

                    return evict.then(organizationRepository.save(org))
                            .flatMap(savedOrg -> emailServicePort
                                    .sendApiKeyDeletedNotification(savedOrg.getEmail(), savedOrg.getDisplayName())
                                    .thenReturn(savedOrg));
                })
                .doOnSuccess(o -> log.info("[apikey] Clé révoquée pour org={}", organizationId))
                .then();
    }
}
