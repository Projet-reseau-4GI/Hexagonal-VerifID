package com.projects.application.port.out;

import com.projects.domain.model.OrganizationApiKey;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrganizationApiKeyRepositoryPort {
    Mono<OrganizationApiKey> findByApiKeyHash(String apiKeyHash);
    Mono<OrganizationApiKey> findById(Long id);
    Flux<OrganizationApiKey> findByOrganizationId(String organizationId);
    Mono<OrganizationApiKey> save(OrganizationApiKey key);
    Mono<Void> deactivate(Long id);
}
