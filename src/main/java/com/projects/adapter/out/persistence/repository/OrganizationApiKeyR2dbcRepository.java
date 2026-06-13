package com.projects.adapter.out.persistence.repository;

import com.projects.adapter.out.persistence.entity.OrganizationApiKeyEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrganizationApiKeyR2dbcRepository extends ReactiveCrudRepository<OrganizationApiKeyEntity, Long> {
    Mono<OrganizationApiKeyEntity> findByApiKeyHash(String apiKeyHash);
    Flux<OrganizationApiKeyEntity> findByOrganizationId(String organizationId);

    @Modifying
    @Query("UPDATE organization_api_keys SET active = false WHERE id = :id")
    Mono<Void> deactivateById(Long id);
}
