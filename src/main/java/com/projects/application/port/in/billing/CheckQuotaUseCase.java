package com.projects.application.port.in.billing;

import com.projects.domain.model.QuotaStatus;
import reactor.core.publisher.Mono;

/**
 * Port d'entrée pour la vérification et la gestion du quota journalier.
 */
public interface CheckQuotaUseCase {

    /**
     * Vérifie si l'organisation peut effectuer une vérification supplémentaire.
     *
     * @param organizationId identifiant de l'organisation
     * @return {@code true} si le quota est disponible, {@code false} sinon
     */
    Mono<Boolean> isQuotaAvailable(String organizationId);

    /**
     * Retourne le snapshot complet du quota journalier de l'organisation.
     *
     * @param organizationId identifiant de l'organisation
     * @return QuotaStatus avec consumed, limit, resetAt et exceeded
     */
    Mono<QuotaStatus> getQuotaStatus(String organizationId);

    /**
     * Incrémente le compteur journalier après une vérification réussie.
     *
     * @param organizationId identifiant de l'organisation
     * @return Mono vide
     */
    Mono<Void> incrementQuota(String organizationId);
}
