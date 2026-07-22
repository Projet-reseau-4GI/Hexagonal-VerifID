package com.projects.adapter.in.web;

import com.projects.application.port.in.billing.CheckQuotaUseCase;
import com.projects.application.port.out.OrganizationRepositoryPort;
import com.projects.application.port.out.VerificationLogRepositoryPort;
import com.projects.application.service.admin.ExportLogsService;
import com.projects.adapter.in.web.dto.OrganizationSummaryDto;
import com.projects.domain.model.Organization;
import com.projects.domain.model.Plan;
import com.projects.domain.model.VerificationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Admin / Super-Admin controller for API Key traceability.
 *
 * Provides endpoints to:
 * - List all API keys for a given organization
 * - View all verification logs for a given organization (by orgId)
 * - View all verification logs for a specific API key
 * - Deactivate (revoke) an API key
 * - Create a new labeled API key for an organization
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
        private final CheckQuotaUseCase checkQuotaUseCase;
        private final ExportLogsService exportLogsService;

        // ─── ORGANISATIONS LIST (NEW) ─────────────────────────────────────────────

        @GetMapping("/organizations")
        @Operation(summary = "Liste paginée des organisations avec quota et statut API key")
        public Mono<ResponseEntity<java.util.List<OrganizationSummaryDto>>> listOrganizations(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                int safeSize = Math.min(size, 100);
                return organizationRepository.findAll()
                                .skip((long) page * safeSize)
                                .take(safeSize)
                                .flatMap(org -> enrichWithQuota(org))
                                .collectList()
                                .map(ResponseEntity::ok);
        }

        @GetMapping("/organizations/{orgId}")
        @Operation(summary = "Détail d'une organisation (admin)")
        public Mono<ResponseEntity<OrganizationSummaryDto>> getOrganization(@PathVariable String orgId) {
                return organizationRepository.findById(UUID.fromString(orgId))
                                .flatMap(org -> enrichWithQuota(org))
                                .map(ResponseEntity::ok)
                                .defaultIfEmpty(ResponseEntity.notFound().build());
        }

        private Mono<OrganizationSummaryDto> enrichWithQuota(Organization org) {
                return checkQuotaUseCase.getQuotaStatus(org.getId().toString())
                                .map(qs -> new OrganizationSummaryDto(
                                                org.getId(),
                                                org.getName(),
                                                org.getDisplayName(),
                                                org.getEmail(),
                                                org.getPlan(),
                                                qs.consumed(),
                                                qs.limit(),
                                                Boolean.TRUE.equals(org.getApiKeyActive()),
                                                org.getStatus(),
                                                org.getCreatedAt()))
                                .onErrorReturn(new OrganizationSummaryDto(
                                                org.getId(),
                                                org.getName(),
                                                org.getDisplayName(),
                                                org.getEmail(),
                                                org.getPlan(),
                                                0L,
                                                Plan.fromString(org.getPlan()).getDailyLimit(),
                                                Boolean.TRUE.equals(org.getApiKeyActive()),
                                                org.getStatus(),
                                                org.getCreatedAt()));
        }

        // ─── ORGANISATION LOGS (PAGINATED, NEW) ──────────────────────────────────

        @GetMapping("/organizations/{orgId}/logs")
        @Operation(summary = "Logs de vérification paginés d'une organisation")
        public Mono<ResponseEntity<java.util.List<VerificationLogResponse>>> getOrgLogs(
                        @PathVariable String orgId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                int safeSize = Math.min(size, 100);
                return verificationLogRepository.findByPlatformId(UUID.fromString(orgId))
                                .sort((a, b) -> b.getDate().compareTo(a.getDate()))
                                .skip((long) page * safeSize)
                                .take(safeSize)
                                .map(this::toResponse)
                                .collectList()
                                .map(ResponseEntity::ok);
        }

        // ─── EXPORT LOGS (NEW) ───────────────────────────────────────────────────

        @GetMapping("/organizations/{orgId}/export")
        @Operation(summary = "Export CSV ou PDF des logs d'une organisation sur une période")
        public Mono<ResponseEntity<byte[]>> exportLogs(
                        @PathVariable String orgId,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                        @RequestParam(defaultValue = "csv") String format) {
                UUID orgUuid = UUID.fromString(orgId);
                if ("pdf".equalsIgnoreCase(format)) {
                        return exportLogsService.exportPdf(orgUuid, startDate, endDate)
                                .map(bytes -> ResponseEntity.ok()
                                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"logs-" + orgId + ".pdf\"")
                                        .contentType(MediaType.APPLICATION_PDF)
                                        .body(bytes));
                }
                return exportLogsService.exportCsv(orgUuid, startDate, endDate)
                        .map(bytes -> ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                        "attachment; filename=\"logs-" + orgId + ".csv\"")
                                .contentType(MediaType.parseMediaType("text/csv"))
                                .body(bytes));
        }

        // ─── GLOBAL DASHBOARD (ENHANCED) ─────────────────────────────────────────

        @GetMapping("/dashboard")
        @Operation(summary = "Métriques globales : organisations actives, vérifications du jour, taux de rejet 7j")
        public Mono<ResponseEntity<Map<String, Object>>> getGlobalDashboard() {
                LocalDateTime startOfDay = LocalDateTime.now(ZoneOffset.UTC).with(java.time.LocalTime.MIN);
                LocalDateTime sevenDaysAgo = LocalDateTime.now(ZoneOffset.UTC).minusDays(7);

                return Mono.zip(
                        // Total active orgs
                        organizationRepository.findAll()
                                .filter(o -> "ACTIVE".equals(o.getStatus()))
                                .count(),
                        // Verifications today (all orgs)
                        verificationLogRepository.findAll()
                                .filter(vl -> vl.getDate() != null && !vl.getDate().isBefore(startOfDay))
                                .count(),
                        // Rejected in last 7 days
                        verificationLogRepository.findAll()
                                .filter(vl -> vl.getDate() != null && !vl.getDate().isBefore(sevenDaysAgo)
                                        && "REJECTED".equals(vl.getStatus()))
                                .count(),
                        // Total in last 7 days
                        verificationLogRepository.findAll()
                                .filter(vl -> vl.getDate() != null && !vl.getDate().isBefore(sevenDaysAgo))
                                .count()
                ).flatMap(tuple -> {
                        long activeOrgs = tuple.getT1();
                        long verToday = tuple.getT2();
                        long rejected7d = tuple.getT3();
                        long total7d = tuple.getT4();
                        double rejectRate = total7d > 0 ? (double) rejected7d / total7d * 100.0 : 0.0;

                        // Plan distribution
                        return organizationRepository.findAll()
                                .filter(o -> "ACTIVE".equals(o.getStatus()))
                                .collectMultimap(o -> o.getPlan() != null ? o.getPlan() : "UNKNOWN",
                                        o -> o.getId())
                                .map(planMap -> {
                                        Map<String, Object> planDist = new LinkedHashMap<>();
                                        planMap.forEach((plan, ids) -> planDist.put(plan, ids.size()));

                                        Map<String, Object> body = new LinkedHashMap<>();
                                        body.put("activeOrganizations", activeOrgs);
                                        body.put("verificationsToday", verToday);
                                        body.put("planDistribution", planDist);
                                        body.put("rejectionRateLast7Days", String.format("%.1f%%", rejectRate));
                                        body.put("rejectedLast7Days", rejected7d);
                                        body.put("totalLast7Days", total7d);
                                        return ResponseEntity.ok(body);
                                });
                });
        }

        @GetMapping("/organizations/{orgId}/api-key")
        @Operation(summary = "Afficher la clé API d'une organisation")
        public Mono<ResponseEntity<ApiKeyResponse>> getApiKey(@PathVariable String orgId) {
                return organizationRepository.findById(java.util.UUID.fromString(orgId))
                                .filter(org -> org.getApiKeyHash() != null)
                                .map(org -> ResponseEntity.ok(new ApiKeyResponse(
                                                org.getId().toString(),
                                                org.getApiKeyLabel(),
                                                org.getApiKeyActive(),
                                                org.getApiKeyCreatedAt())))
                                .defaultIfEmpty(ResponseEntity.notFound().build());
        }

        @PostMapping("/organizations/{orgId}/api-key")
        @Operation(summary = "Générer une nouvelle clé API pour une organisation (écrase l'ancienne)")
        public Mono<ResponseEntity<ApiKeyCreatedResponse>> generateApiKey(
                        @PathVariable String orgId,
                        @RequestBody CreateApiKeyRequest request) {

                return organizationRepository.findById(java.util.UUID.fromString(orgId))
                                .flatMap(org -> {
                                        String rawKey = "vfid_"
                                                        + java.util.UUID.randomUUID().toString().replace("-", "");
                                        String hashedKey = com.projects.adapter.out.security.SecurityUtils
                                                        .hashApiKey(rawKey);

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
                                                                                        saved.getApiKeyCreatedAt())));
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
                                .map(org -> ResponseEntity
                                                .ok("Clé API désactivée avec succès pour l'organisation " + orgId))
                                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
        }

        // ─── VERIFICATION LOGS ───────────────────────────────────────────────────

        @GetMapping("/organizations/{orgId}/verifications")
        @Operation(summary = "Lister tous les logs de vérification d'une organisation")
        public Flux<VerificationLogResponse> getOrgVerifications(
                        @PathVariable String orgId,
                        @RequestParam(required = false) String status) {
                return verificationLogRepository.findByPlatformId(UUID.fromString(orgId))
                                .filter(log -> status == null || status.equalsIgnoreCase(log.getStatus()))
                                .map(this::toResponse);
        }

        @GetMapping("/organizations/{orgId}/stats")
        @Operation(summary = "Statistiques rapides d'une organisation (admin)")
        public Mono<OrgStatsResponse> getOrgStats(@PathVariable String orgId) {
                LocalDateTime startOfDay = LocalDateTime.now().with(java.time.LocalTime.MIN);
                return Mono.zip(
                                verificationLogRepository.countByPlatformIdAndDateAfter(UUID.fromString(orgId),
                                                startOfDay),
                                verificationLogRepository.findByPlatformId(UUID.fromString(orgId)).count(),
                                organizationRepository.findById(java.util.UUID.fromString(orgId))
                                                .map(org -> Boolean.TRUE.equals(org.getApiKeyActive()) ? 1L : 0L)
                                                .defaultIfEmpty(0L))
                                .map(tuple -> new OrgStatsResponse(
                                                orgId,
                                                tuple.getT1(), // verifications today
                                                tuple.getT2(), // total verifications
                                                tuple.getT3() // active API keys (0 or 1)
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
                                log.getExpiryDate());
        }

        public record ApiKeyResponse(
                        String organizationId,
                        String label,
                        Boolean active,
                        LocalDateTime createdAt) {
        }

        public record ApiKeyCreatedResponse(
                        String organizationId,
                        String label,
                        String rawApiKey,
                        LocalDateTime createdAt) {
        }

        public record CreateApiKeyRequest(String label) {
        }

        public record OrgStatsResponse(
                        String organizationId,
                        Long verificationsToday,
                        Long totalVerifications,
                        Long activeApiKeys) {
        }

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
                        String expiryDate) {
        }
}
