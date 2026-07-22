package com.projects.application.service.billing;

import com.projects.adapter.in.web.dto.PlanPricingDto;
import com.projects.adapter.out.persistence.entity.PlanPricingEntity;
import com.projects.adapter.out.persistence.repository.PlanPricingRepository;
import com.projects.application.port.in.billing.ManagePlanPricingUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagePlanPricingUseCaseImpl implements ManagePlanPricingUseCase {

    private final PlanPricingRepository planPricingRepository;

    @Override
    public Flux<PlanPricingDto> getAllPlanPrices() {
        return planPricingRepository.findAll()
                .map(this::toDto)
                .switchIfEmpty(Flux.defer(() -> {
                    // Fallback par défaut si la table est vide
                    return Flux.just(
                            new PlanPricingDto("FREE", 0.00, "XOF", LocalDateTime.now()),
                            new PlanPricingDto("PREMIUM", 5000.00, "XOF", LocalDateTime.now()),
                            new PlanPricingDto("MAX", 15000.00, "XOF", LocalDateTime.now())
                    );
                }));
    }

    @Override
    public Mono<PlanPricingDto> updatePlanPrice(String planId, Double newPrice, String currency) {
        log.info("Updating plan price for planId={} to {}{}", planId, newPrice, currency);

        return planPricingRepository.findById(planId)
                .flatMap(entity -> {
                    entity.setPrice(newPrice);
                    entity.setCurrency(currency);
                    entity.setUpdatedAt(LocalDateTime.now());
                    return planPricingRepository.save(entity);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Si le forfait n'existe pas encore dans la base, on le crée
                    PlanPricingEntity newEntity = PlanPricingEntity.builder()
                            .planId(planId)
                            .price(newPrice)
                            .currency(currency)
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return planPricingRepository.save(newEntity);
                }))
                .map(this::toDto);
    }

    private PlanPricingDto toDto(PlanPricingEntity entity) {
        return new PlanPricingDto(
                entity.getPlanId(),
                entity.getPrice(),
                entity.getCurrency(),
                entity.getUpdatedAt()
        );
    }
}
