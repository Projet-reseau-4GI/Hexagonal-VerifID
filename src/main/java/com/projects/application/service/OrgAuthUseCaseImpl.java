package com.projects.application.service;

import com.projects.adapter.in.web.dto.*;
import com.projects.application.port.in.OrgAuthUseCase;
import com.projects.application.port.out.EmailServicePort;
import com.projects.application.port.out.KernelAuthPort;
import com.projects.application.port.out.OrganizationRepositoryPort;
import com.projects.application.service.admin.LocalJwtService;
import com.projects.domain.model.Organization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

/**
 * Implémentation autonome du UseCase d'authentification des organisations.
 *
 * AUCUN appel au Kernel RT-Comops. Tout est géré localement :
 * - Inscription + vérification email via OTP (Brevo)
 * - Connexion email + mot de passe → JWT local (HS256)
 * - Génération de clientId unique
 * - Réinitialisation de mot de passe via OTP
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrgAuthUseCaseImpl implements OrgAuthUseCase {

    private final OrganizationRepositoryPort organizationRepository;
    private final EmailServicePort emailService;
    private final LocalJwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final KernelAuthPort kernelAuthPort;

    // ─────────────────────────────────────────────────────────────────────────
    // INSCRIPTION
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<Void> register(OrgRegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("[auth] Début d'inscription pour email={}", email);

        return organizationRepository.existsByEmail(email)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException(
                                "Un compte existe déjà pour cet email : " + email));
                    }

                    String otp = generateOtp();
                    LocalDateTime otpExpiry = LocalDateTime.now().plusMinutes(15);
                    String passwordHash = passwordEncoder.encode(request.getPassword());
                    String clientId = "cli-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

                    Organization org = Organization.builder()
                            .id(UUID.randomUUID())
                            .email(email)
                            .name(request.getOrganizationName())
                            .displayName(request.getDisplayName() != null
                                    ? request.getDisplayName()
                                    : request.getOrganizationName())
                            .logoUri(request.getLogoUri())
                            .plan("FREEMIUM")
                            .status("PENDING")
                            .isEmailVerified(false)
                            .passwordHash(passwordHash)
                            .clientId(clientId)
                            .otpCode(otp)
                            .otpExpiry(otpExpiry)
                            .dailyVerificationCount(0)
                            .apiKeyActive(false)
                            .createdAt(LocalDateTime.now())
                            .build();

                    return organizationRepository.save(org)
                            .flatMap(saved -> emailService.sendOtp(email, otp, saved.getDisplayName())
                                    .doOnSuccess(v -> log.info("[auth] OTP d'inscription envoyé à {}", email))
                                    .onErrorResume(ex -> {
                                        log.warn("[auth] Échec d'envoi de l'OTP pour {} : {}. Le compte reste créé pour test local.", email, ex.getMessage());
                                        return Mono.empty();
                                    }))
                            .then();
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VÉRIFICATION EMAIL + ACTIVATION
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<OrgAuthResponse> verifyEmailAndActivate(OrgVerifyEmailRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("[auth] Vérification OTP pour email={}", email);

        return organizationRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new RuntimeException("Organisation introuvable pour : " + email)))
                .flatMap(org -> {
                    if (Boolean.TRUE.equals(org.getIsEmailVerified())) {
                        return Mono.error(new IllegalStateException("Email déjà vérifié. Utilisez la connexion."));
                    }
                    if (org.getOtpCode() == null || !org.getOtpCode().equals(request.getOtpCode())) {
                        return Mono.error(new IllegalArgumentException("Code OTP incorrect."));
                    }
                    if (org.getOtpExpiry() == null || org.getOtpExpiry().isBefore(LocalDateTime.now())) {
                        return Mono.error(
                                new IllegalArgumentException("Code OTP expiré. Veuillez recommencer l'inscription."));
                    }

                    // Activer le compte
                    org.setIsEmailVerified(true);
                    org.setStatus("ACTIVE");
                    org.setOtpCode(null);
                    org.setOtpExpiry(null);

                    return organizationRepository.save(org)
                            .map(saved -> buildAuthResponse(saved, jwtService.generateToken(saved.getEmail(), "ORG")));
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONNEXION
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<OrgAuthResponse> login(OrgLoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("[auth] Tentative de connexion pour email={}", email);

        return organizationRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new RuntimeException("Identifiants incorrects.")))
                .flatMap(org -> {
                    if (!Boolean.TRUE.equals(org.getIsEmailVerified())) {
                        return Mono.error(new IllegalStateException(
                                "Veuillez d'abord vérifier votre email avant de vous connecter."));
                    }
                    if (!"ACTIVE".equals(org.getStatus())) {
                        return Mono.error(new IllegalStateException("Compte suspendu ou désactivé."));
                    }
                    if (!passwordEncoder.matches(request.getPassword(), org.getPasswordHash())) {
                        return Mono.error(new IllegalArgumentException("Identifiants incorrects."));
                    }

                    String token = jwtService.generateToken(org.getEmail(), "ORG");
                    log.info("[auth] Connexion réussie pour org={}", org.getId());
                    return Mono.just(buildAuthResponse(org, token));
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MOT DE PASSE OUBLIÉ (compat ancien flux OTP)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<Void> initiateAuth(OrgInitiateAuthRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("[auth] Demande de réinitialisation/OTP pour email={}", email);

        return organizationRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new RuntimeException("Aucun compte trouvé pour cet email.")))
                .flatMap(org -> {
                    String otp = generateOtp();
                    org.setOtpCode(otp);
                    org.setOtpExpiry(LocalDateTime.now().plusMinutes(15));
                    return organizationRepository.save(org)
                            .flatMap(saved -> emailService.sendPasswordReset(email, otp, saved.getDisplayName()))
                            .then();
                });
    }

    @Override
    public Mono<OrgAuthResponse> completeAuth(OrgVerifyOtpRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("[auth] Validation OTP (flux reset) pour email={}", email);

        return organizationRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new RuntimeException("Organisation introuvable pour : " + email)))
                .flatMap(org -> {
                    if (org.getOtpCode() == null || !org.getOtpCode().equals(request.getOtpCode())) {
                        return Mono.error(new IllegalArgumentException("Code OTP incorrect."));
                    }
                    if (org.getOtpExpiry() == null || org.getOtpExpiry().isBefore(LocalDateTime.now())) {
                        return Mono.error(new IllegalArgumentException("Code OTP expiré."));
                    }

                    org.setOtpCode(null);
                    org.setOtpExpiry(null);
                    // Si le compte est en PENDING et qu'il confirme via OTP, on l'active
                    if ("PENDING".equals(org.getStatus())) {
                        org.setIsEmailVerified(true);
                        org.setStatus("ACTIVE");
                    }

                    return organizationRepository.save(org)
                            .map(saved -> buildAuthResponse(saved, jwtService.generateToken(saved.getEmail(), "ORG")));
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TWIN AUTHENTICATION (KERNEL)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Mono<TwinAuthStep1Response> kernelLogin(KernelLoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("[auth] Tentative de connexion via KSM Kernel pour email={}", email);

        return kernelAuthPort.login(email, request.getPassword())
                .flatMap(tokenResponse -> {
                    String kernelToken = tokenResponse.getAccessToken();
                    return kernelAuthPort.getMyOrganizations(kernelToken)
                            .map(orgs -> TwinAuthStep1Response.builder()
                                    .kernelToken(kernelToken)
                                    .availableOrganizations(orgs)
                                    .build());
                })
                .onErrorMap(ex -> new RuntimeException("Échec de l'authentification Kernel: " + ex.getMessage()));
    }

    @Override
    public Mono<OrgAuthResponse> selectOrganization(SelectOrgRequest request) {
        log.info("[auth] Sélection de l'organisation via Twin Authentication pour orgId={}", request.getOrganizationId());
        
        // 1. Récupérer les orgs du user via son token Kernel pour valider l'appartenance
        return kernelAuthPort.getMyOrganizations(request.getKernelToken())
                .flatMap(orgs -> {
                    var selectedOrgOpt = orgs.stream()
                            .filter(o -> o.getId().equals(request.getOrganizationId()))
                            .findFirst();

                    if (selectedOrgOpt.isEmpty()) {
                        return Mono.error(new IllegalStateException("L'utilisateur n'appartient pas à cette organisation."));
                    }

                    var kernelOrg = selectedOrgOpt.get();

                    // 2. Synchroniser ou récupérer l'organisation localement
                    return organizationRepository.findById(kernelOrg.getId())
                            .switchIfEmpty(Mono.defer(() -> {
                                log.info("[auth] Synchronisation de la nouvelle organisation depuis le Kernel : {}", kernelOrg.getId());
                                Organization newOrg = Organization.builder()
                                        .id(kernelOrg.getId())
                                        .email(kernelOrg.getEmail() != null ? kernelOrg.getEmail() : "no-email@kernel.com")
                                        .name(kernelOrg.getShortName() != null ? kernelOrg.getShortName() : "Org")
                                        .displayName(kernelOrg.getDisplayName() != null ? kernelOrg.getDisplayName() : kernelOrg.getShortName())
                                        .logoUri(kernelOrg.getLogoUri())
                                        .plan("FREEMIUM")
                                        .status("ACTIVE")
                                        .isEmailVerified(true) // Déjà vérifié via Kernel
                                        // On génère un mot de passe local aléatoire car on gère l'auth via Kernel
                                        .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString())) 
                                        .clientId("cli-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                                        .dailyVerificationCount(0)
                                        .apiKeyActive(false)
                                        .createdAt(LocalDateTime.now())
                                        .build();
                                return organizationRepository.save(newOrg);
                            }))
                            .flatMap(localOrg -> {
                                // 3. Générer le JWT local VerifID pour l'organisation sélectionnée
                                String localToken = jwtService.generateToken(localOrg.getEmail(), "ORG");
                                log.info("[auth] Twin Authentication réussie. JWT local généré pour org={}", localOrg.getId());
                                return Mono.just(buildAuthResponse(localOrg, localToken));
                            });
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers privés
    // ─────────────────────────────────────────────────────────────────────────

    private OrgAuthResponse buildAuthResponse(Organization org, String token) {
        return OrgAuthResponse.builder()
                .token(token)
                .organizationId(org.getId())
                .organizationName(org.getDisplayName())
                .email(org.getEmail())
                .plan(org.getPlan())
                .logoUri(org.getLogoUri())
                .clientId(org.getClientId())
                .status(org.getStatus())
                .build();
    }

    private String generateOtp() {
        // OTP à 6 chiffres
        return String.format("%06d", new Random().nextInt(999999));
    }
}
