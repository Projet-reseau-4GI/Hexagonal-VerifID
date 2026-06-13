package com.projects.adapter.out.persistence.adapter;

import com.projects.adapter.out.persistence.mapper.OrganizationApiKeyMapper;
import com.projects.adapter.out.persistence.repository.OrganizationApiKeyR2dbcRepository;
import com.projects.application.port.out.OrganizationApiKeyRepositoryPort;
import com.projects.domain.model.OrganizationApiKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class OrganizationApiKeyRepositoryAdapter implements OrganizationApiKeyRepositoryPort {

    private final OrganizationApiKeyR2dbcRepository repository;

    @Override
    public Mono<OrganizationApiKey> findByApiKeyHash(String apiKeyHash) {
        return repository.findByApiKeyHash(apiKeyHash)
                .map(OrganizationApiKeyMapper::toDomain);
    }

    @Override
    public Mono<OrganizationApiKey> findById(Long id) {
        return repository.findById(id)
                .map(OrganizationApiKeyMapper::toDomain);
    }

    @Override
    public Flux<OrganizationApiKey> findByOrganizationId(String organizationId) {
        return repository.findByOrganizationId(organizationId)
                .map(OrganizationApiKeyMapper::toDomain);
    }

    @Override
    public Mono<OrganizationApiKey> save(OrganizationApiKey key) {
        return repository.save(OrganizationApiKeyMapper.toEntity(key))
                .map(OrganizationApiKeyMapper::toDomain);
    }

    @Override
    public Mono<Void> deactivate(Long id) {
        return repository.deactivateById(id);
    }
}
