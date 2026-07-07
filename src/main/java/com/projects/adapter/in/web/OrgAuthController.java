package com.projects.adapter.in.web;

import com.projects.adapter.in.web.dto.*;
import com.projects.application.port.in.OrgAuthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Contrôleur REST d'authentification des organisations VerifID.
 *
 * Mode autonome (Legacy) et Mode Twin Authentication (Kernel).
 *
 * Flux d'inscription (2 étapes) :
 * POST /api/org/auth/register → enregistre l'org + envoie OTP par email
 * POST /api/org/auth/verify-email → vérifie l'OTP + active le compte + retourne
 * JWT
 *
 * Connexion directe :
 * POST /api/org/auth/login → email + mot de passe → JWT local
 *
 * Réinitialisation de mot de passe :
 * POST /api/org/auth/initiate → envoie OTP de reset par email
 * POST /api/org/auth/verify-otp → valide l'OTP + retourne JWT (compat ancien
 * flux)
 */
@RestController
@RequestMapping("/api/org/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Organisation Auth", description = "Authentification autonome des organisations (sans Kernel)")
public class OrgAuthController {

    private final OrgAuthUseCase orgAuthUseCase;

    // ─────────────────────────────────────────────────────────────────────────
    // INSCRIPTION
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/register")
    @Operation(summary = "Étape 1/2 : Inscription — enregistre l'organisation et envoie un OTP par email")
    public Mono<ResponseEntity<String>> register(@Valid @RequestBody OrgRegisterRequest request) {
        return orgAuthUseCase.register(request)
                .then(Mono.just(ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body("Compte créé. Un code de vérification a été envoyé à " + request.getEmail() + ".")));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Étape 2/2 : Vérifie l'OTP + active le compte + retourne le JWT et les infos de l'organisation")
    public Mono<ResponseEntity<OrgAuthResponse>> verifyEmail(@Valid @RequestBody OrgVerifyEmailRequest request) {
        return orgAuthUseCase.verifyEmailAndActivate(request)
                .map(ResponseEntity::ok);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONNEXION
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    @Operation(summary = "Connexion directe par email + mot de passe → JWT local VerifID")
    public Mono<ResponseEntity<OrgAuthResponse>> login(@Valid @RequestBody OrgLoginRequest request) {
        return orgAuthUseCase.login(request)
                .map(ResponseEntity::ok);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MOT DE PASSE OUBLIÉ (compatibilité ancien flux OTP)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/initiate")
    @Operation(summary = "Mot de passe oublié — envoie un OTP de réinitialisation par email")
    public Mono<ResponseEntity<String>> initiateAuth(@Valid @RequestBody OrgInitiateAuthRequest request) {
        return orgAuthUseCase.initiateAuth(request)
                .then(Mono.just(ResponseEntity.ok(
                        "Si un compte existe pour cet email, un code OTP a été envoyé.")));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Valide l'OTP de réinitialisation et retourne un JWT (compatibilité)")
    public Mono<ResponseEntity<OrgAuthResponse>> verifyOtp(@Valid @RequestBody OrgVerifyOtpRequest request) {
        return orgAuthUseCase.completeAuth(request)
                .map(ResponseEntity::ok);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TWIN AUTHENTICATION (KERNEL)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/kernel-login")
    @Operation(summary = "Étape 1 (Twin Auth) : Connexion via le KSM Kernel et récupération des organisations")
    public Mono<ResponseEntity<TwinAuthStep1Response>> kernelLogin(@Valid @RequestBody KernelLoginRequest request) {
        return orgAuthUseCase.kernelLogin(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/select-org")
    @Operation(summary = "Étape 2 (Twin Auth) : Sélection d'une organisation et génération du JWT local VerifID")
    public Mono<ResponseEntity<OrgAuthResponse>> selectOrganization(@Valid @RequestBody SelectOrgRequest request) {
        return orgAuthUseCase.selectOrganization(request)
                .map(ResponseEntity::ok);
    }
}
