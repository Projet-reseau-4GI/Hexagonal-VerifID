package com.projects.application.port.out;

import com.projects.domain.model.Organization;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port sortant pour la persistance locale des organisations VerifID.
 *
 * Les organisations sont synchronisées depuis le Kernel (organization-core).
 * L'ID est le même UUID que dans le Kernel (BD répartie).
 */
public interface OrganizationRepositoryPort {

    /**
     * Sauvegarde ou met à jour une organisation (upsert par id).
     */
    Mono<Organization> save(Organization organization);

    /**
     * Recherche une organisation par son UUID (= UUID Kernel).
     */
    Mono<Organization> findById(UUID id);

    /**
     * Recherche une organisation par son email de contact.
     * Utilisé pour vérifier si l'org est déjà synchronisée localement.
     */
    Mono<Organization> findByEmail(String email);

    /**
     * Vérifie l'existence d'une organisation par email.
     */
    Mono<Boolean> existsByEmail(String email);

    /**
     * Recherche une organisation par le hash de sa clé API.
     */
    Mono<Organization> findByApiKeyHash(String apiKeyHash);

    /**
     * Retourne toutes les organisations (utilisé pour les opérations de masse
     * comme le reset du compteur journalier).
     */
    Flux<Organization> findAll();
}
