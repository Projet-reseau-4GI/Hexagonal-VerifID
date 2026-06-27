package com.projects.adapter.out.kernel;

import com.projects.config.kernel.KernelClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.projects.application.port.out.AuthGatewayPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

/**
 * Adaptateur HTTP vers le kernel-core et l'auth-core du Kernel RT-Comops.
 *
 * Responsabilités :
 *  1. Enregistrer VerifID comme ClientApplication dans le kernel (bootstrap initial).
 *  2. Authentifier un utilisateur via l'auth-core (login tenant-scopé).
 *  3. Découvrir les contextes de login disponibles pour un email.
 *
 * Endpoints utilisés :
 *   POST /api/kernel/client-applications   → enregistrement ClientApplication
 *   POST /api/auth/login                   → authentification utilisateur
 *   POST /api/auth/discover-login-contexts → découverte des contextes de login
 */
@Service
@Slf4j
public class KernelAuthService implements AuthGatewayPort {

    private static final String CLIENT_APP_ENDPOINT    = "/api/kernel/client-applications";
    private static final String AUTH_IDENTIFY_ENDPOINT = "/api/auth/identify";
    private static final String AUTH_LOGIN_ENDPOINT    = "/api/auth/login";
    private static final String AUTH_DISCOVER_ENDPOINT = "/api/auth/discover-contexts";

    private final WebClient kernelWebClient;
    private final KernelClientProperties kernelProperties;

    public KernelAuthService(
            @Qualifier("kernelWebClient") WebClient kernelWebClient,
            KernelClientProperties kernelProperties) {
        this.kernelWebClient = kernelWebClient;
        this.kernelProperties = kernelProperties;
    }

    /**
     * Enregistre VerifID comme ClientApplication dans le kernel.
     * À appeler une seule fois lors du bootstrap (via admin ou script d'init).
     *
     * @param adminBearerToken Token JWT d'un administrateur kernel
     * @param tenantId         UUID du tenant cible
     * @return Réponse du kernel avec clientId et secret
     */
    @Override
    @CircuitBreaker(name = "kernel-api", fallbackMethod = "fallbackRegisterClientApplication")
    public Mono<ClientAppRegistrationDTO> registerClientApplication(
            String adminBearerToken,
            UUID tenantId) {

        Map<String, Object> body = Map.of(
                "clientId", kernelProperties.getClientId(),
                "name", "VerifID Backend",
                "description", "Service de validation de documents d'identité",
                "allowedServiceCodes", Set.of(kernelProperties.getServiceCode()),
                "systemManaged", false
        );

        return kernelWebClient.post()
                .uri(CLIENT_APP_ENDPOINT)
                .headers(h -> {
                    h.setContentType(MediaType.APPLICATION_JSON);
                    if (tenantId != null) h.set("X-Tenant-Id", tenantId.toString());
                    if (adminBearerToken != null) h.setBearerAuth(adminBearerToken);
                })
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Object data = response.get("data");
                    if (data instanceof Map<?, ?> d) {
                        return new ClientAppRegistrationDTO(
                                parseString(d, "clientId"),
                                parseString(d, "plainSecret"),
                                parseString(d, "status")
                        );
                    }
                    throw new IllegalStateException("Réponse kernel inattendue lors de l'enregistrement");
                })
                .doOnSuccess(r -> log.info("[kernel-core] ClientApplication enregistrée — clientId={}", r.clientId()))
                .doOnError(e -> log.error("[kernel-core] Erreur enregistrement ClientApplication : {}", e.getMessage()));
    }

    /**
     * Étape 1 : Demande au Kernel d'envoyer un OTP à cet email.
     */
    @Override
    @CircuitBreaker(name = "kernel-api", fallbackMethod = "fallbackInitiateOtp")
    public Mono<Void> initiateOtp(String email) {
        Map<String, Object> body = Map.of("principal", email);

        return kernelWebClient.post()
                .uri(AUTH_IDENTIFY_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(r -> log.info("[auth-core] OTP demandé avec succès pour : {}", email))
                .doOnError(e -> log.error("[auth-core] Erreur lors de la demande d'OTP : {}", e.getMessage()));
    }

    /**
     * Étape 2 : Vérifie l'OTP (comme password) auprès du Kernel et récupère le profil (UserAccountResponse).
     */
    @Override
    @CircuitBreaker(name = "kernel-api", fallbackMethod = "fallbackVerifyOtpAndLogin")
    public Mono<AuthLoginDTO> verifyOtpAndLogin(String email, String otpCode, UUID tenantId) {
        Map<String, Object> body = Map.of(
                "principal", email,
                "password", otpCode
        );

        return kernelWebClient.post()
                .uri(AUTH_LOGIN_ENDPOINT)
                .headers(h -> {
                    h.setContentType(MediaType.APPLICATION_JSON);
                    if (tenantId != null) {
                        h.set("X-Tenant-Id", tenantId.toString());
                    }
                })
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Object data = response.get("data");
                    if (data instanceof Map<?, ?> d) {
                        return new AuthLoginDTO(
                                parseString(d, "accessToken"),
                                parseString(d, "refreshToken"),
                                parseUuid(d, "userId"),
                                parseString(d, "email"),
                                parseString(d, "username"),
                                parseString(d, "plan"),
                                parseOrganizations(d.get("organizations"))
                        );
                    }
                    throw new IllegalStateException("Réponse auth-core inattendue");
                })
                .doOnError(e -> log.error("[auth-core] Erreur vérification OTP pour {} : {}", email, e.getMessage()));
    }

    /**
     * Découvre les contextes de login disponibles pour un email donné.
     */
    public Mono<Map<String, Object>> discoverLoginContexts(String email) {
        Map<String, Object> body = Map.of("email", email);

        return kernelWebClient.post()
                .uri(AUTH_DISCOVER_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(m -> (Map<String, Object>) m)
                .doOnError(e -> log.error("[auth-core] Erreur découverte contextes : {}", e.getMessage()));
    }

    // Utilitaires
    // ----------------------------------------------------------------

    private UUID parseUuid(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? UUID.fromString(val.toString()) : null;
    }

    private String parseString(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private java.util.List<OrgRef> parseOrganizations(Object orgsObj) {
        if (!(orgsObj instanceof java.util.List<?> list)) return java.util.List.of();
        
        return list.stream()
                .filter(o -> o instanceof Map<?, ?>)
                .map(o -> (Map<?, ?>) o)
                .map(m -> new OrgRef(
                        parseUuid(m, "organizationId"),
                        parseString(m, "shortName"),
                        parseString(m, "displayName")
                ))
                .toList();
    }

    // Fallbacks Resilience4j
    // ----------------------------------------------------------------

    public Mono<ClientAppRegistrationDTO> fallbackRegisterClientApplication(String adminBearerToken, UUID tenantId, Throwable t) {
        log.error("[kernel-core] Circuit Breaker ouvert ou erreur Kernel pour registerClientApplication (tenant={}). Erreur: {}", tenantId, t.getMessage());
        return Mono.error(new IllegalStateException("Service Kernel indisponible. Impossible d'enregistrer l'application."));
    }

    public Mono<Void> fallbackInitiateOtp(String email, Throwable t) {
        log.error("[auth-core] Circuit Breaker ouvert ou erreur Kernel pour initiateOtp (email={}). Erreur: {}", email, t.getMessage());
        return Mono.error(new IllegalStateException("Service d'authentification indisponible. Impossible d'envoyer l'OTP."));
    }

    public Mono<AuthLoginDTO> fallbackVerifyOtpAndLogin(String email, String otpCode, UUID tenantId, Throwable t) {
        log.error("[auth-core] Circuit Breaker ouvert ou erreur Kernel pour verifyOtpAndLogin (email={}). Erreur: {}", email, t.getMessage());
        return Mono.error(new IllegalStateException("Service d'authentification indisponible. Impossible de valider l'OTP."));
    }
}
