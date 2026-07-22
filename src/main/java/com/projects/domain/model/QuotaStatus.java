package com.projects.domain.model;

import java.time.Instant;

/**
 * Snapshot de l'état du quota journalier d'une organisation.
 *
 * @param organizationId identifiant de l'organisation
 * @param plan           nom du plan courant (FREE, PREMIUM, MAX)
 * @param consumed       nombre de vérifications effectuées aujourd'hui
 * @param limit          limite journalière du plan (0 = plan FREE bloqué)
 * @param resetAt        instant de réinitialisation du compteur (minuit UTC)
 * @param exceeded       true si consumed >= limit (ou plan FREE)
 */
public record QuotaStatus(
        String organizationId,
        String plan,
        long consumed,
        long limit,
        Instant resetAt,
        boolean exceeded
) {
    /** Retourne le pourcentage d'utilisation du quota (0–100+). */
    public double usagePercent() {
        if (limit == 0) return 100.0;
        return (double) consumed / limit * 100.0;
    }
}
