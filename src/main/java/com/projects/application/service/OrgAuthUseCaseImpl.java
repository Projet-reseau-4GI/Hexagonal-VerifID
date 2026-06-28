package com.projects.application.service;

import com.projects.adapter.in.web.dto.OrgAuthResponse;
import com.projects.adapter.in.web.dto.OrgInitiateAuthRequest;
import com.projects.adapter.in.web.dto.OrgVerifyOtpRequest;
import com.projects.application.port.in.OrgAuthUseCase;
import com.projects.application.port.out.AuthGatewayPort;
import com.projects.application.port.out.OrganizationGatewayPort;
import com.projects.application.port.out.OrganizationRepositoryPort;
import com.projects.domain.model.Organization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrgAuthUseCaseImpl implements OrgAuthUseCase {

    private final AuthGatewayPort authGateway;
    private final OrganizationGatewayPort orgGateway;
    private final OrganizationRepositoryPort organizationRepository;

    @Override
    public Mono<Void> initiateAuth(OrgInitiateAuthRequest request) {
        String email = request.getEmail();
        log.info("[org-auth] Initiation de l'authentification pour : {}", email);

        // On cherche d'abord en base locale (notre cache VerifID persistant).
        // Si elle n'y est pas, on interroge le Kernel.
        return organizationRepository.findByEmail(email)
                .map(org -> true)
                .switchIfEmpty(
                        orgGateway.searchOrganizationByEmail(email, null)
                                .map(orgSummary -> true)
                )
                .switchIfEmpty(Mono.error(new RuntimeException("Organisation introuvable pour cet email")))
                .flatMap(found -> {
                    log.info("[org-auth] Organisation trouvée pour : {}", email);
                    return authGateway.initiateOtp(email);
                });
    }

    @Override
    public Mono<OrgAuthResponse> completeAuth(OrgVerifyOtpRequest request) {
        String email = request.getEmail();
        log.info("[org-auth] Validation OTP pour : {}", email);

        // Le tenantId sera résolu côté Kernel par l'email. On passe null.
        return authGateway.verifyOtpAndLogin(email, request.getOtpCode(), null)
                .flatMap(loginResponse -> {
                    // Vérifier que l'utilisateur a au moins une organisation
                    if (loginResponse.organizations() == null || loginResponse.organizations().isEmpty()) {
                        return Mono.error(new RuntimeException("Aucune organisation liée à ce compte."));
                    }

                    // On prend la première organisation de la liste.
                    // Si l'utilisateur appartient à plusieurs orgs, le Kernel gérera ça, 
                    // mais VerifID synchronisera la première par défaut.
                    var orgRef = loginResponse.organizations().get(0);
                    
                    // On récupère les détails complets de cette organisation depuis le Kernel
                    // (On utilise le token fraîchement récupéré pour s'authentifier).
                    return orgGateway.getOrganization(null, orgRef.organizationId(), loginResponse.accessToken())
                            .flatMap(orgSummary -> syncOrganizationLocal(orgSummary, loginResponse))
                            .map(savedOrg -> OrgAuthResponse.builder()
                                    .token(loginResponse.accessToken()) // On retourne le token du Kernel
                                    .organizationId(savedOrg.getId())
                                    .organizationName(savedOrg.getDisplayName())
                                    .email(loginResponse.email())
                                    .plan(savedOrg.getPlan())
                                    .logoUri(savedOrg.getLogoUri())
                                    .build());
                });
    }

    /**
     * Synchronise l'organisation du Kernel vers la base de données locale (VerifID).
     */
    private Mono<Organization> syncOrganizationLocal(OrganizationGatewayPort.OrganizationSummaryDTO orgSummary, AuthGatewayPort.AuthLoginDTO loginResponse) {
        return organizationRepository.findById(orgSummary.id())
                .flatMap(existingOrg -> {
                    // L'organisation existe déjà, on met à jour les infos de base
                    existingOrg.setName(orgSummary.name());
                    existingOrg.setDisplayName(orgSummary.displayName());
                    existingOrg.setLogoUri(orgSummary.logoUri());
                    existingOrg.setLastSyncedAt(LocalDateTime.now());
                    return organizationRepository.save(existingOrg);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Première connexion, on crée l'organisation locale
                    Organization newOrg = Organization.builder()
                            .id(orgSummary.id()) // ID du Kernel
                            .email(loginResponse.email())
                            .name(orgSummary.name())
                            .displayName(orgSummary.displayName())
                            .logoUri(orgSummary.logoUri())
                            .plan("FREEMIUM") // Forfait initial par défaut
                            .dailyVerificationCount(0)
                            .createdAt(LocalDateTime.now())
                            .lastSyncedAt(LocalDateTime.now())
                            // Les clés API seront créées plus tard par l'utilisateur
                            .apiKeyActive(true)
                            .build();
                    
                    log.info("[org-auth] Création de l'organisation locale (ID Kernel) : {}", newOrg.getId());
                    return organizationRepository.save(newOrg);
                }));
    }
}
