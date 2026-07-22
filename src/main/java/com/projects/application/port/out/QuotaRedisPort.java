package com.projects.application.port.out;

import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Port de sortie pour la gestion du compteur de quota journalier via Redis.
 *
 * Clé Redis : {@code quota:{orgId}:{yyyyMMdd}}
 * TTL : 25 heures (couvre le reset minuit UTC + marge de sécurité).
 */
public interface QuotaRedisPort {

    /**
     * Incrémente atomiquement le compteur journalier et retourne la nouvelle valeur.
     *
     * @param orgId identifiant de l'organisation
     * @param date  date cible (UTC)
     * @return la nouvelle valeur du compteur après incrément
     */
    Mono<Long> incrementAndGet(String orgId, LocalDate date);

    /**
     * Retourne la valeur courante du compteur sans l'incrémenter.
     * Retourne 0L si la clé n'existe pas.
     *
     * @param orgId identifiant de l'organisation
     * @param date  date cible (UTC)
     * @return la valeur courante du compteur, ou 0 si absente
     */
    Mono<Long> getCurrentCount(String orgId, LocalDate date);

    /**
     * Supprime la clé Redis du compteur journalier (utile pour les tests / reset manuel).
     *
     * @param orgId identifiant de l'organisation
     * @param date  date cible (UTC)
     * @return Mono vide
     */
    Mono<Void> deleteKey(String orgId, LocalDate date);
}
