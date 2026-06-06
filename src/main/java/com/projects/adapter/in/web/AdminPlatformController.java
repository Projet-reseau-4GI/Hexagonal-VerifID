package com.projects.adapter.in.web;

import com.projects.adapter.in.web.dto.PlatformTokenStatsDto;
import com.projects.application.port.in.admin.ManagePlatformAdminUseCase;
import com.projects.domain.model.Platform;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Super-admin controller for platform management.
 * Injects ManagePlatformAdminUseCase — fully hexagonal, no legacy service dependency.
 */
@RestController
@RequestMapping("/api/admin/platforms")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Administration", description = "Gestion des plateformes — réservé aux super-admins")
public class AdminPlatformController {

    private final ManagePlatformAdminUseCase managePlatformAdminUseCase;

    @GetMapping
    @Operation(summary = "Lister toutes les plateformes",
               description = "Retourne l'ensemble des plateformes enregistrées dans le système.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public Flux<Platform> getAllPlatforms() {
        log.info("Admin: listing all platforms");
        return managePlatformAdminUseCase.getAllPlatforms();
    }

    @PostMapping("/{id}/toggle-status")
    @Operation(summary = "Activer ou désactiver une plateforme",
               description = "Inverse l'état actif/inactif d'une plateforme.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public Mono<Platform> toggleStatus(@PathVariable Long id) {
        log.info("Admin: toggling status for platform id={}", id);
        return managePlatformAdminUseCase.toggleStatus(id);
    }

    @GetMapping("/tokens")
    @Operation(summary = "Obtenir les statistiques des tokens API",
               description = "Retourne la liste des plateformes avec le nombre d'appels et la date du dernier appel.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public Flux<PlatformTokenStatsDto> getPlatformTokenStats(
            @RequestParam(required = false) String search) {
        log.info("Admin: listing platform token stats with search={}", search);
        return managePlatformAdminUseCase.getPlatformTokenStats(search);
    }
}
