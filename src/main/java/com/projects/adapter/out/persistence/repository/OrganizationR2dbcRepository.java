package com.projects.adapter.out.persistence.repository;

import com.projects.adapter.out.persistence.entity.OrganizationEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repository R2DBC pour la table `organizations`.
 * L'ID de l'entité est un UUID issu du Kernel (BD répartie).
 */
public interface OrganizationR2dbcRepository extends ReactiveCrudRepository<OrganizationEntity, UUID> {

    /**
     * Recherche une organisation par son email de contact.
     * Utilisé pour vérifier si l'org est déjà synchronisée localement.
     */
    Mono<OrganizationEntity> findByEmail(String email);

    /**
     * Vérifie l'existence d'une organisation par email.
     */
    @Query("SELECT COUNT(*) > 0 FROM organizations WHERE email = :email")
    Mono<Boolean> existsByEmail(String email);

    /**
     * Recherche une organisation par le hash de sa clé API.
     */
    Mono<OrganizationEntity> findByApiKeyHash(String apiKeyHash);
}
