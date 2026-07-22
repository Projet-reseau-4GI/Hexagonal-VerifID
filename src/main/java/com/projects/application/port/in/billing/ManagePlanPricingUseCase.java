package com.projects.application.port.in.billing;

import com.projects.adapter.in.web.dto.PlanPricingDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ManagePlanPricingUseCase {

    /**
     * Récupère les prix de tous les forfaits disponibles.
     */
    Flux<PlanPricingDto> getAllPlanPrices();

    /**
     * Met à jour le prix d'un forfait spécifique.
     *
     * @param planId L'identifiant du forfait (ex: PREMIUM, MAX)
     * @param newPrice Le nouveau prix
     * @param currency La devise (ex: XOF)
     */
    Mono<PlanPricingDto> updatePlanPrice(String planId, Double newPrice, String currency);

}
