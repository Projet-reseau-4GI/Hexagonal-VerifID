package com.projects.adapter.in.web;

import com.projects.application.port.out.OrganizationRepositoryPort;
import com.projects.application.port.out.VerificationLogRepositoryPort;
import com.projects.domain.model.Organization;
import com.projects.domain.model.VerificationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Admin / Super-Admin controller for API Key traceability.
 *
 * Provides endpoints to:
 *  - List all API keys for a given organization
 *  - View all verification logs for a given organization (by orgId)
 *  - View all verification logs for a specific API key
 *  - Deactivate (revoke) an API key
 *  - Create a new labeled API key for an organization
 */
@RestController
@RequestMapping("/api/admin/traceability")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "Admin Traceability", description = "Super-Admin : traçabilité des clés API et vérifications par organisation")
public class AdminTraceabilityController {

    private final OrganizationRepositoryPort organizationRepository;
    private final VerificationLogRepositoryPort verificationLogRepository;

    // ─── API KEYS ────────────────────────────────────────────────────────────

    @GetMapping("/organizations/{orgId}/api-key")
    @Operation(summary = "Afficher la clé API d'une organisation")
    public Mono<ResponseEntity<ApiKeyResponse>> getApiKey(@PathVariable String orgId) {
        return organizationRepository.findById(java.util.UUID.fromString(orgId))
                .filter(org -> org.getApiKeyHash() != null)
                .map(org -> ResponseEntity.ok(new ApiKeyResponse(
                        org.getId().toString(),
                        org.getApiKeyLabel(),
                        org.getApiKeyActive(),
                        org.getApiKeyCreatedAt()
                )))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/organizations/{orgId}/api-key")
    @Operation(summary = "Générer une nouvelle clé API pour une organisation (écrase l'ancienne)")
    public Mono<ResponseEntity<ApiKeyCreatedResponse>> generateApiKey(
            @PathVariable String orgId,
            @RequestBody CreateApiKeyRequest request) {

        return organizationRepository.findById(java.util.UUID.fromString(orgId))
                .flatMap(org -> {
                    String rawKey = "vfid_" + java.util.UUID.randomUUID().toString().replace("-", "");
                    String hashedKey = com.projects.adapter.out.security.SecurityUtils.hashApiKey(rawKey);

                    org.setApiKeyHash(hashedKey);
                    org.setApiKeyLabel(request.label() != null ? request.label() : "Clé API");
                    org.setApiKeyActive(true);
                    org.setApiKeyCreatedAt(LocalDateTime.now());

                    return organizationRepository.save(org)
                            .map(saved -> ResponseEntity.status(HttpStatus.CREATED)
                                    .body(new ApiKeyCreatedResponse(
                                            saved.getId().toString(),
                                            saved.getApiKeyLabel(),
                                            rawKey,
                                            saved.getApiKeyCreatedAt()
                                    )));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @DeleteMapping("/organizations/{orgId}/api-key/deactivate")
    @Operation(summary = "Désactiver la clé API d'une organisation")
    public Mono<ResponseEntity<String>> deactivateApiKey(@PathVariable String orgId) {
        return organizationRepository.findById(java.util.UUID.fromString(orgId))
                .flatMap(org -> {
                    org.setApiKeyActive(false);
                    return organizationRepository.save(org);
                })
                .map(org -> ResponseEntity.ok("Clé API désactivée avec succès pour l'organisation " + orgId))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    // ─── VERIFICATION LOGS ───────────────────────────────────────────────────

    @GetMapping("/organizations/{orgId}/verifications")
    @Operation(summary = "Lister tous les logs de vérification d'une organisation")
    public Flux<VerificationLogResponse> getOrgVerifications(
            @PathVariable String orgId,
            @RequestParam(required = false) String status) {
        return verificationLogRepository.findByPlatformId(orgId)
                .filter(log -> status == null || status.equalsIgnoreCase(log.getStatus()))
                .map(this::toResponse);
    }



    @GetMapping("/organizations/{orgId}/stats")
    @Operation(summary = "Statistiques rapides d'une organisation (admin)")
    public Mono<OrgStatsResponse> getOrgStats(@PathVariable String orgId) {
        LocalDateTime startOfDay = LocalDateTime.now().with(java.time.LocalTime.MIN);
        return Mono.zip(
                verificationLogRepository.countByPlatformIdAndDateAfter(orgId, startOfDay),
                verificationLogRepository.findByPlatformId(orgId).count(),
                organizationRepository.findById(java.util.UUID.fromString(orgId))
                        .map(org -> Boolean.TRUE.equals(org.getApiKeyActive()) ? 1L : 0L)
                        .defaultIfEmpty(0L)
        ).map(tuple -> new OrgStatsResponse(
                orgId,
                tuple.getT1(),   // verifications today
                tuple.getT2(),   // total verifications
                tuple.getT3()    // active API keys (0 or 1)
        ));
    }

    // ─── GLOBAL ADMIN ────────────────────────────────────────────────────────

    @GetMapping("/verifications/recent")
    @Operation(summary = "Super-Admin : 50 dernières vérifications toutes organisations confondues")
    public Flux<VerificationLogResponse> getAllRecentVerifications() {
        return verificationLogRepository.findAll()
                .sort((a, b) -> b.getDate().compareTo(a.getDate()))
                .take(50)
                .map(this::toResponse);
    }

    // ─── RECORDS (DTOs internes) ──────────────────────────────────────────────

    private VerificationLogResponse toResponse(VerificationLog log) {
        return new VerificationLogResponse(
                log.getId(),
                log.getPlatformId(),
                log.getDate(),
                log.getDocType(),
                log.getStatus(),
                log.getReason(),
                log.getConfidence(),
                log.getDocumentNumber(),
                log.getHolderName(),
                log.getDateOfBirth(),
                log.getIssueDate(),
                log.getExpiryDate()
        );
    }

    public record ApiKeyResponse(
            String organizationId,
            String label,
            Boolean active,
            LocalDateTime createdAt
    ) {}

    public record ApiKeyCreatedResponse(
            String organizationId,
            String label,
            String rawApiKey,
            LocalDateTime createdAt
    ) {}

    public record CreateApiKeyRequest(String label) {}

    public record OrgStatsResponse(
            String organizationId,
            Long verificationsToday,
            Long totalVerifications,
            Long activeApiKeys
    ) {}

    public record VerificationLogResponse(
            Long id,
            String organizationId,
            LocalDateTime date,
            String docType,
            String status,
            String reason,
            Double confidence,
            String documentNumber,
            String holderName,
            String dateOfBirth,
            String issueDate,
            String expiryDate
    ) {}
}
