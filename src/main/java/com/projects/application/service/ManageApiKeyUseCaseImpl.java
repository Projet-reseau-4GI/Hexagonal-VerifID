package com.projects.application.service;

import com.projects.adapter.in.web.dto.ApiKeyResponse;
import com.projects.adapter.out.security.SecurityUtils;
import com.projects.application.port.in.ManageApiKeyUseCase;
import com.projects.application.port.out.OrganizationRepositoryPort;
import com.projects.application.port.out.EmailServicePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public Mono<ApiKeyResponse> generateApiKey(UUID organizationId, String label) {
        return organizationRepository.findById(organizationId)
                .switchIfEmpty(Mono.error(new RuntimeException("Organisation introuvable")))
                .flatMap(org -> {
                    // Générer une clé brute (ex: vf_live_xxxx)
                    byte[] randomBytes = new byte[32];
                    secureRandom.nextBytes(randomBytes);
                    String rawApiKey = "vf_id_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

                    // Hasher la clé
                    String hashedKey = SecurityUtils.hashApiKey(rawApiKey);

                    // Mettre à jour l'organisation
                    org.setApiKeyHash(hashedKey);
                    org.setApiKeyLabel(label != null && !label.isBlank() ? label : "Default Key");
                    org.setApiKeyActive(true);
                    org.setApiKeyCreatedAt(LocalDateTime.now());

                    return organizationRepository.save(org)
                            .flatMap(savedOrg -> emailServicePort
                                    .sendApiKeyCreatedNotification(savedOrg.getEmail(), savedOrg.getDisplayName())
                                    .thenReturn(savedOrg))
                            .map(savedOrg -> ApiKeyResponse.builder()
                                    .label(savedOrg.getApiKeyLabel())
                                    .apiKey(rawApiKey) // Retournée EN CLAIR une seule fois
                                    .active(savedOrg.getApiKeyActive())
                                    .createdAt(savedOrg.getApiKeyCreatedAt())
                                    .build());
                });
    }

    @Override
    public Mono<Void> revokeApiKey(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .switchIfEmpty(Mono.error(new RuntimeException("Organisation introuvable")))
                .flatMap(org -> {
                    org.setApiKeyActive(false);
                    return organizationRepository.save(org)
                            .flatMap(savedOrg -> emailServicePort
                                    .sendApiKeyDeletedNotification(savedOrg.getEmail(), savedOrg.getDisplayName())
                                    .thenReturn(savedOrg));
                })
                .then();
    }
}
