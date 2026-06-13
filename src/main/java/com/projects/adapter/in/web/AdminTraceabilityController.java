package com.projects.adapter.in.web;

import com.projects.application.port.out.OrganizationApiKeyRepositoryPort;
import com.projects.application.port.out.VerificationLogRepositoryPort;
import com.projects.domain.model.OrganizationApiKey;
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

    private final OrganizationApiKeyRepositoryPort apiKeyRepository;
    private final VerificationLogRepositoryPort verificationLogRepository;

    // ─── API KEYS ────────────────────────────────────────────────────────────

    @GetMapping("/organizations/{orgId}/api-keys")
    @Operation(summary = "Lister toutes les clés API d'une organisation")
    public Flux<ApiKeyResponse> listApiKeys(@PathVariable String orgId) {
        return apiKeyRepository.findByOrganizationId(orgId)
                .map(k -> new ApiKeyResponse(
                        k.getId(),
                        k.getOrganizationId(),
                        k.getLabel(),
                        k.getActive(),
                        k.getCreatedAt()
                ));
    }

    @PostMapping("/organizations/{orgId}/api-keys")
    @Operation(summary = "Créer une nouvelle clé API pour une organisation")
    public Mono<ResponseEntity<ApiKeyCreatedResponse>> createApiKey(
            @PathVariable String orgId,
            @RequestBody CreateApiKeyRequest request) {

        // Generate a secure random API key
        String rawKey = "vfid_" + java.util.UUID.randomUUID().toString().replace("-", "");
        String hashedKey = com.projects.adapter.out.security.SecurityUtils.hashApiKey(rawKey);

        OrganizationApiKey apiKey = OrganizationApiKey.builder()
                .organizationId(orgId)
                .apiKeyHash(hashedKey)
                .label(request.label() != null ? request.label() : "Clé API")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        return apiKeyRepository.save(apiKey)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(new ApiKeyCreatedResponse(
                                saved.getId(),
                                saved.getOrganizationId(),
                                saved.getLabel(),
                                rawKey, // Only returned ONCE at creation
                                saved.getCreatedAt()
                        )));
    }

    @DeleteMapping("/api-keys/{keyId}/deactivate")
    @Operation(summary = "Révoquer (désactiver) une clé API")
    public Mono<ResponseEntity<String>> deactivateApiKey(@PathVariable Long keyId) {
        return apiKeyRepository.deactivate(keyId)
                .thenReturn(ResponseEntity.ok("Clé API " + keyId + " désactivée avec succès."));
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

    @GetMapping("/api-keys/{keyId}/verifications")
    @Operation(summary = "Lister tous les logs de vérification associés à une clé API spécifique")
    public Flux<VerificationLogResponse> getApiKeyVerifications(@PathVariable Long keyId) {
        return verificationLogRepository.findByApiKeyId(keyId)
                .map(this::toResponse);
    }

    @GetMapping("/organizations/{orgId}/stats")
    @Operation(summary = "Statistiques rapides d'une organisation (admin)")
    public Mono<OrgStatsResponse> getOrgStats(@PathVariable String orgId) {
        LocalDateTime startOfDay = LocalDateTime.now().with(java.time.LocalTime.MIN);
        return Mono.zip(
                verificationLogRepository.countByPlatformIdAndDateAfter(orgId, startOfDay),
                verificationLogRepository.findByPlatformId(orgId).count(),
                apiKeyRepository.findByOrganizationId(orgId)
                        .filter(k -> Boolean.TRUE.equals(k.getActive())).count()
        ).map(tuple -> new OrgStatsResponse(
                orgId,
                tuple.getT1(),   // verifications today
                tuple.getT2(),   // total verifications
                tuple.getT3()    // active API keys
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
                log.getApiKeyId(),
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
            Long id,
            String organizationId,
            String label,
            Boolean active,
            LocalDateTime createdAt
    ) {}

    public record ApiKeyCreatedResponse(
            Long id,
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
            Long apiKeyId,
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
