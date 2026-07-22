package com.projects.adapter.in.web;

import com.projects.adapter.in.web.dto.PlanPricingDto;
import com.projects.adapter.in.web.dto.UpdatePlanPriceRequest;
import com.projects.application.port.in.billing.ManagePlanPricingUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "Plan Pricing", description = "Gestion de la tarification des forfaits (Public & Admin)")
public class PlanPricingController {

    private final ManagePlanPricingUseCase managePlanPricingUseCase;

    // ─── ENDPOINT PUBLIC ───────────────────────────────────────────────────

    @GetMapping("/api/plans/pricing")
    @Operation(summary = "Lister les prix actuels de tous les forfaits")
    public Mono<ResponseEntity<List<PlanPricingDto>>> getAllPlanPrices() {
        return managePlanPricingUseCase.getAllPlanPrices()
                .collectList()
                .map(ResponseEntity::ok);
    }

    // ─── ENDPOINT ADMIN ────────────────────────────────────────────────────

    @PutMapping("/api/admin/plans/{planId}/price")
    @Operation(summary = "Super-Admin : Modifier le prix d'un forfait")
    public Mono<ResponseEntity<PlanPricingDto>> updatePlanPrice(
            @PathVariable String planId,
            @Valid @RequestBody UpdatePlanPriceRequest request) {
        
        return managePlanPricingUseCase.updatePlanPrice(planId.toUpperCase(), request.getPrice(), request.getCurrency())
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Erreur lors de la mise à jour du prix du forfait {}", planId, e);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
}
