package com.projects.application.service.billing;

import com.projects.adapter.in.web.dto.MobilePaymentRequest;
import com.projects.adapter.in.web.dto.MobilePaymentResponse;
import com.projects.application.port.in.billing.ProcessPaymentUseCase;
import com.projects.application.port.out.EmailServicePort;
import com.projects.application.port.out.OrganizationRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessPaymentUseCaseImpl implements ProcessPaymentUseCase {

    private final OrganizationRepositoryPort organizationRepositoryPort;
    private final EmailServicePort emailServicePort;

    @Override
    public Mono<Void> processSuccessfulPayment(String organizationId, String planId, String paymentId) {
        log.info("Processing successful payment {} for org {} with new plan {}", paymentId, organizationId, planId);

        UUID orgUuid;
        try {
            orgUuid = UUID.fromString(organizationId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid organizationId format: {}", organizationId);
            return Mono.error(e);
        }

        return organizationRepositoryPort.findById(orgUuid)
                .switchIfEmpty(Mono.error(new RuntimeException("Organization not found locally to update plan")))
                .flatMap(org -> {
                    org.setPlan(planId); // Update to new plan locally
                    return organizationRepositoryPort.save(org)
                            .flatMap(savedOrg -> emailServicePort
                                    .sendPaymentConfirmation(savedOrg.getEmail(), savedOrg.getDeveloperName(), planId)
                                    .thenReturn(savedOrg));
                })
                .doOnSuccess(savedOrg -> log.info("Successfully upgraded plan for org {} to {} locally", organizationId,
                        planId))
                .then();
    }

    @Override
    public Mono<MobilePaymentResponse> initiateMobilePayment(UUID organizationId, MobilePaymentRequest request) {
        log.info("Initiating Mobile Payment for org {} to plan {} via operator {}",
                organizationId, request.getPlanId(), request.getOperator());

        return organizationRepositoryPort.findById(organizationId)
                .switchIfEmpty(Mono.error(new RuntimeException("Organisation introuvable.")))
                .flatMap(org -> {
                    // TODO: Connecter à la vraie Gateway de paiement (ex: FedaPay, Campay, etc.)
                    // Ici on simule une réponse d'API qui demande au client de taper un code USSD

                    String fakeTxId = "tx_mb_" + UUID.randomUUID().toString().substring(0, 8);

                    MobilePaymentResponse response = MobilePaymentResponse.builder()
                            .transactionId(fakeTxId)
                            .status("PROMPT_SENT")
                            .message("Veuillez consulter votre téléphone (" + request.getPhoneNumber()
                                    + ") et valider le paiement par code secret.")
                            .build();

                    return Mono.just(response);
                });
    }
}
