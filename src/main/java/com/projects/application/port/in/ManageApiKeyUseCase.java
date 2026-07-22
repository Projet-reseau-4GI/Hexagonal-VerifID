package com.projects.application.port.in;

import com.projects.adapter.in.web.dto.ApiKeyResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ManageApiKeyUseCase {

    /**
     * Génère une nouvelle clé API pour une organisation.
     * Remplace la clé précédente si elle existe.
     * @param organizationId l'ID de l'organisation
     * @param label un label optionnel pour la clé
     * @return ApiKeyResponse contenant la clé en clair (une seule fois)
     */
    Mono<ApiKeyResponse> generateApiKey(UUID organizationId, String label);

    /**
     * Rotation de la clé API : génère une nouvelle clé, invalide l'ancienne.
     * @return la nouvelle clé en clair (une seule fois)
     */
    Mono<ApiKeyResponse> rotateApiKey(UUID organizationId);

    /**
     * Révocation de la clé API pour l'organisation.
     */
    Mono<Void> revokeApiKey(UUID organizationId);
}
