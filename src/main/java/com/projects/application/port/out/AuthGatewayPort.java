package com.projects.application.port.out;

import reactor.core.publisher.Mono;
import java.util.List;
import java.util.UUID;

/**
 * Port sortant pour la communication avec le module d'authentification (auth-core du Kernel).
 *
 * Workflow OTP organisations (2 étapes) :
 *   1. initiateOtp(email)          → Kernel envoie un OTP par email
 *   2. verifyOtpAndLogin(email, otp) → Kernel valide, retourne JWT + infos user + orgs
 */
public interface AuthGatewayPort {

    /**
     * Étape 1 — Soumet l'email au Kernel pour déclencher l'envoi de l'OTP.
     * Endpoint : POST /api/auth/identify { principal: email }
     *
     * @param email Email de l'utilisateur organisation
     * @return Mono<Void> — success si le Kernel a bien reçu la demande
     */
    Mono<Void> initiateOtp(String email);

    /**
     * Étape 2 — Valide l'OTP et récupère le JWT + les informations utilisateur.
     * Endpoint : POST /api/auth/login { principal: email, password: otpCode, tenantId }
     *
     * @param email    Email de l'utilisateur
     * @param otpCode  Code OTP reçu par email
     * @param tenantId TenantId extrait du contexte (null = auto-detect via discoverContexts)
     * @return Mono<AuthLoginDTO> — JWT + user info + liste des organisations
     */
    Mono<AuthLoginDTO> verifyOtpAndLogin(String email, String otpCode, UUID tenantId);

    /**
     * Enregistre VerifID comme ClientApplication dans le Kernel (bootstrap admin uniquement).
     */
    Mono<ClientAppRegistrationDTO> registerClientApplication(String adminBearerToken, UUID tenantId);

    // ── DTOs ───────────────────────────────────────────────────────────────────

    record ClientAppRegistrationDTO(String clientId, String plainSecret, String status) {}

    /**
     * Résultat de la validation OTP (verifyOtpAndLogin).
     *
     * @param accessToken    JWT retourné par le Kernel (à transmettre au client)
     * @param refreshToken   Refresh token Kernel
     * @param userId         UUID de l'utilisateur dans le Kernel (= ID local Admin)
     * @param email          Email de l'utilisateur
     * @param username       Nom d'affichage du compte Kernel
     * @param plan           Plan actuel dans le Kernel (peut différer du plan VerifID)
     * @param organizations  Liste des organisations liées à cet utilisateur
     */
    record AuthLoginDTO(
            String accessToken,
            String refreshToken,
            UUID userId,
            String email,
            String username,
            String plan,
            List<OrgRef> organizations
    ) {}

    /**
     * Référence légère à une organisation retournée par le Kernel.
     */
    record OrgRef(
            UUID organizationId,
            String shortName,
            String displayName
    ) {}
}

