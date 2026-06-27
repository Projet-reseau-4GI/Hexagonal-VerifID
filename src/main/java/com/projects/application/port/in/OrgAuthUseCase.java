package com.projects.application.port.in;

import com.projects.adapter.in.web.dto.OrgAuthResponse;
import com.projects.adapter.in.web.dto.OrgInitiateAuthRequest;
import com.projects.adapter.in.web.dto.OrgVerifyOtpRequest;
import reactor.core.publisher.Mono;

/**
 * Port d'entrée pour l'authentification des organisations VerifID
 * via le Kernel RT-Comops.
 */
public interface OrgAuthUseCase {

    /**
     * Étape 1 : Demande l'envoi d'un OTP par email via le Kernel,
     * tout en vérifiant en parallèle que l'organisation existe.
     */
    Mono<Void> initiateAuth(OrgInitiateAuthRequest request);

    /**
     * Étape 2 : Vérifie l'OTP, récupère les informations utilisateur/organisation
     * du Kernel, et synchronise ces données dans la base de données locale.
     */
    Mono<OrgAuthResponse> completeAuth(OrgVerifyOtpRequest request);
}
