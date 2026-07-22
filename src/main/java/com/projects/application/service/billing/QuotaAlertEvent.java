package com.projects.application.service.billing;

/**
 * Événement interne publié quand une organisation approche ou atteint son quota.
 *
 * @param organizationId identifiant de l'organisation
 * @param plan           plan tarifaire courant
 * @param consumed       nombre de vérifications effectuées
 * @param limit          limite journalière du plan
 * @param thresholdPercent 90 pour l'alerte à 90%, 100 pour l'alerte de dépassement
 */
public record QuotaAlertEvent(
        String organizationId,
        String plan,
        long consumed,
        long limit,
        int thresholdPercent
) {}
