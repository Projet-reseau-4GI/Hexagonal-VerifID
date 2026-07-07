package com.projects.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Réponse unifiée après connexion ou inscription complète d'une organisation.
 * Contient le JWT local (signé par VerifID), le clientId unique et les infos de
 * l'org.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgAuthResponse {

    /** JWT local VerifID (HS256, durée configurable, sans dépendance au Kernel). */
    private String token;

    /** UUID interne de l'organisation. */
    private UUID organizationId;

    /** Nom d'affichage de l'organisation. */
    private String organizationName;

    /** Email de contact. */
    private String email;

    /** Plan tarifaire actuel : FREEMIUM, STARTER, PRO. */
    private String plan;

    /** URL du logo. */
    private String logoUri;

    /**
     * Identifiant client unique de l'organisation.
     * À utiliser pour s'identifier lors des appels API.
     */
    private String clientId;

    /** Statut du compte : PENDING, ACTIVE. */
    private String status;
}
