package com.projects.application.port.in;

import com.projects.adapter.in.web.dto.*;
import reactor.core.publisher.Mono;

/**
 * Port d'entrée pour l'authentification LOCALE des organisations VerifID.
 *
 * Ce port remplace complètement la dépendance au Kernel RT-Comops.
 * Le backend VerifID gère désormais de façon autonome :
 * - L'inscription avec vérification email par OTP
 * - La connexion par email + mot de passe
 * - La génération de JWT local (HS256)
 * - La réinitialisation de mot de passe
 */
public interface OrgAuthUseCase {

    /**
     * Étape 1/2 de l'inscription : enregistre l'organisation (statut PENDING)
     * et envoie un OTP de vérification par email.
     */
    Mono<Void> register(OrgRegisterRequest request);

    /**
     * Étape 2/2 de l'inscription : vérifie l'OTP, active le compte (statut ACTIVE)
     * et retourne le JWT local + les informations de l'organisation.
     */
    Mono<OrgAuthResponse> verifyEmailAndActivate(OrgVerifyEmailRequest request);

    /**
     * Connexion directe par email + mot de passe.
     * Retourne le JWT local si les identifiants sont corrects et le compte est
     * actif.
     */
    Mono<OrgAuthResponse> login(OrgLoginRequest request);

    /**
     * Étape 1/2 du mot de passe oublié : envoie un OTP de réinitialisation.
     */
    Mono<Void> initiateAuth(OrgInitiateAuthRequest request);

    /**
     * Étape 2/2 du mot de passe oublié (via OTP) : retourne un JWT si l'OTP est
     * valide.
     * Compatible avec l'ancien flux OTP pour ne pas casser les intégrations
     * existantes.
     */
    Mono<OrgAuthResponse> completeAuth(OrgVerifyOtpRequest request);
}
