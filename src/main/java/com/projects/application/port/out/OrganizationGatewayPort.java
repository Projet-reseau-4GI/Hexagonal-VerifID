package com.projects.application.port.out;

import reactor.core.publisher.Mono;
import java.util.UUID;

/**
 * Port sortant pour la communication avec le module d'organisation (organization-core du Kernel).
 *
 * Utilisé pour :
 *   - Rechercher une organisation par email (lors de l'initiation de l'auth)
 *   - Récupérer les détails d'une organisation par son ID
 *   - Vérifier l'abonnement à un service
 *   - Mettre à jour le plan d'une organisation
 */
public interface OrganizationGatewayPort {

    /**
     * Recherche une organisation dans le Kernel par email de contact.
     * Endpoint : GET /api/organizations/search?q={email}
     * Appelé en parallèle de initiateOtp pour valider l'accès avant envoi du OTP.
     *
     * @param email       Email de recherche
     * @param bearerToken Token optionnel (peut être null pour une recherche publique)
     * @return Mono vide si aucune organisation trouvée
     */
    Mono<OrganizationSummaryDTO> searchOrganizationByEmail(String email, String bearerToken);

    /**
     * Récupère les détails d'une organisation par son UUID.
     * Endpoint : GET /api/organizations/{organizationId}
     */
    Mono<OrganizationSummaryDTO> getOrganization(UUID tenantId, UUID orgId, String bearerToken);

    /**
     * Vérifie que l'organisation est abonnée au service KYC.
     * Endpoint : GET /api/organizations/{orgId}/service-entitlements
     */
    Mono<Boolean> isSubscribedToService(UUID tenantId, UUID orgId, String serviceCode, String bearerToken);

    /**
     * Demande la mise à jour du plan d'une organisation dans le Kernel.
     */
    Mono<Void> upgradeOrganizationPlan(UUID tenantId, UUID orgId, String newPlan);

    // ── DTO ─────────────────────────────────────────────────────────────────────

    /**
     * Résumé d'une organisation tel que retourné par le Kernel.
     *
     * @param id          UUID de l'organisation (= ID local VerifID)
     * @param tenantId    UUID du tenant
     * @param name        Nom court (shortName)
     * @param displayName Nom d'affichage
     * @param email       Email de contact
     * @param logoUri     URL du logo
     * @param code        Code organisation
     * @param status      Statut (ACTIVE, SUSPENDED, ...)
     * @param plan        Plan kernel (peut différer du plan VerifID local)
     */
    record OrganizationSummaryDTO(
            UUID id,
            UUID tenantId,
            String name,
            String displayName,
            String email,
            String logoUri,
            String code,
            String status,
            String plan
    ) {}
}

