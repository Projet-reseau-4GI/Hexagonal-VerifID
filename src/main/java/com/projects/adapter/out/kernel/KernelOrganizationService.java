package com.projects.adapter.out.kernel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.projects.application.port.out.OrganizationGatewayPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cache.annotation.Cacheable;

/**
 * Adaptateur HTTP vers l'organization-core du Kernel RT-Comops.
 *
 * Responsabilités :
 *  1. Vérifier qu'une organisation est bien abonnée au service KYC
 *     avant d'autoriser une vérification de document.
 *  2. Récupérer les informations d'une organisation.
 *
 * Endpoint utilisé :
 *   GET /api/organizations/{orgId}/service-entitlements
 *
 * Codes d'erreur kernel possibles :
 *   ORGANIZATION_SERVICE_NOT_SUBSCRIBED → org non abonnée
 *   ORGANIZATION_SERVICE_QUOTA_EXCEEDED → quota fair-use dépassé
 */
@Service
@Slf4j
public class KernelOrganizationService implements OrganizationGatewayPort {

    private static final String ORGANIZATIONS_BASE = "/api/organizations";

    private final WebClient kernelWebClient;

    public KernelOrganizationService(@Qualifier("kernelWebClient") WebClient kernelWebClient) {
        this.kernelWebClient = kernelWebClient;
    }

    /**
     * Recherche une organisation par son email.
     */
    @Override
    @CircuitBreaker(name = "kernel-api", fallbackMethod = "fallbackSearchOrganizationByEmail")
    public Mono<OrganizationSummaryDTO> searchOrganizationByEmail(String email, String bearerToken) {
        return kernelWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(ORGANIZATIONS_BASE + "/search")
                        .queryParam("q", email)
                        .build())
                .headers(h -> applyKernelHeaders(h, null, null, bearerToken))
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    Object data = response.get("data");
                    if (data instanceof List<?> orgs && !orgs.isEmpty()) {
                        Object firstOrg = orgs.get(0);
                        if (firstOrg instanceof Map<?, ?> orgMap) {
                            return Mono.just(mapToOrganizationSummary(orgMap));
                        }
                    }
                    return Mono.empty();
                })
                .doOnError(e -> log.error("[org-core] Erreur recherche org avec email={} : {}", email, e.getMessage()));
    }

    /**
     * Vérifie que l'organisation est abonnée au serviceCode donné.
     *
     * @param tenantId    UUID du tenant
     * @param orgId       UUID de l'organisation
     * @param serviceCode Code du service (ex: "KYC")
     * @param bearerToken JWT RS256 de l'utilisateur
     * @return Mono<Boolean> — true si abonnée, false sinon
     */
    @Override
    @CircuitBreaker(name = "kernel-api", fallbackMethod = "fallbackIsSubscribedToService")
    public Mono<Boolean> isSubscribedToService(UUID tenantId, UUID orgId, String serviceCode, String bearerToken) {
        return kernelWebClient.get()
                .uri(ORGANIZATIONS_BASE + "/" + orgId + "/service-entitlements")
                .headers(h -> applyKernelHeaders(h, tenantId, orgId, bearerToken))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Object data = response.get("data");
                    if (data instanceof List<?> services) {
                        return services.stream().anyMatch(s -> {
                            if (s instanceof Map<?, ?> entry) {
                                Object code = entry.get("serviceCode");
                                Object active = entry.get("active");
                                return serviceCode.equals(code) && Boolean.TRUE.equals(active);
                            }
                            return false;
                        });
                    }
                    return false;
                })
                .onErrorResume(e -> {
                    log.warn("[org-core] Impossible de vérifier l'entitlement org={} service={} : {}",
                            orgId, serviceCode, e.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * Récupère les informations de base d'une organisation depuis l'organization-core.
     */
    @Override
    @Cacheable(value = "organizations", key = "#orgId.toString()", unless = "#result == null")
    @CircuitBreaker(name = "kernel-api", fallbackMethod = "fallbackGetOrganization")
    public Mono<OrganizationSummaryDTO> getOrganization(UUID tenantId, UUID orgId, String bearerToken) {
        return kernelWebClient.get()
                .uri(ORGANIZATIONS_BASE + "/" + orgId)
                .headers(h -> applyKernelHeaders(h, tenantId, orgId, bearerToken))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Object data = response.get("data");
                    if (data instanceof Map<?, ?> org) {
                        return mapToOrganizationSummary(org);
                    }
                    throw new IllegalStateException("Réponse organization-core inattendue");
                })
                .doOnError(e -> log.error("[org-core] Erreur récupération org={} : {}", orgId, e.getMessage()));
    }

    /**
     * Demande la mise à jour du forfait d'une organisation via organization-core.
     */
    @Override
    @CircuitBreaker(name = "kernel-api", fallbackMethod = "fallbackUpgradeOrganizationPlan")
    public Mono<Void> upgradeOrganizationPlan(UUID tenantId, UUID orgId, String newPlan) {
        return kernelWebClient.put()
                .uri(ORGANIZATIONS_BASE + "/" + orgId + "/plan")
                .headers(h -> applyKernelHeaders(h, tenantId, orgId, null))
                .bodyValue(Map.of("plan", newPlan))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(e -> log.error("[org-core] Erreur mise à jour plan pour org={} : {}", orgId, e.getMessage()));
    }

    // Utilitaires
    // ----------------------------------------------------------------

    private void applyKernelHeaders(HttpHeaders headers, UUID tenantId, UUID orgId, String bearerToken) {
        if (tenantId != null) {
            headers.set("X-Tenant-Id", tenantId.toString());
        }
        if (orgId != null) {
            headers.set("X-Organization-Id", orgId.toString());
        }
        if (bearerToken != null && !bearerToken.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        }
    }

    private UUID parseUuid(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? UUID.fromString(val.toString()) : null;
    }

    private String parseString(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private OrganizationSummaryDTO mapToOrganizationSummary(Map<?, ?> org) {
        return new OrganizationSummaryDTO(
                parseUuid(org, "id"),
                parseUuid(org, "tenantId"),
                parseString(org, "shortName"),
                parseString(org, "displayName"),
                parseString(org, "email"),
                parseString(org, "logoUri"),
                parseString(org, "code"),
                parseString(org, "status"),
                parseString(org, "plan")
        );
    }

    // Fallbacks Resilience4j
    // ----------------------------------------------------------------

    public Mono<Boolean> fallbackIsSubscribedToService(UUID tenantId, UUID orgId, String serviceCode, String bearerToken, Throwable t) {
        log.error("[org-core] Circuit Breaker ouvert ou erreur Kernel pour isSubscribedToService (org={}). Mode dégradé : refus par défaut. Erreur: {}", orgId, t.getMessage());
        return Mono.just(false);
    }

    public Mono<OrganizationSummaryDTO> fallbackGetOrganization(UUID tenantId, UUID orgId, String bearerToken, Throwable t) {
        log.error("[org-core] Circuit Breaker ouvert ou erreur Kernel pour getOrganization (org={}). Erreur: {}", orgId, t.getMessage());
        return Mono.error(new IllegalStateException("Service Kernel indisponible. Impossible de récupérer l'organisation."));
    }

    public Mono<OrganizationSummaryDTO> fallbackSearchOrganizationByEmail(String email, String bearerToken, Throwable t) {
        log.error("[org-core] Circuit Breaker ouvert ou erreur Kernel pour searchOrganizationByEmail (email={}). Erreur: {}", email, t.getMessage());
        return Mono.error(new IllegalStateException("Service Kernel indisponible. Impossible de rechercher l'organisation."));
    }

    public Mono<Void> fallbackUpgradeOrganizationPlan(UUID tenantId, UUID orgId, String newPlan, Throwable t) {
        log.error("[org-core] Circuit Breaker ouvert ou erreur Kernel pour upgradeOrganizationPlan (org={}). Erreur: {}", orgId, t.getMessage());
        return Mono.error(new IllegalStateException("Service Kernel indisponible. Impossible de mettre à jour le forfait."));
    }
}
