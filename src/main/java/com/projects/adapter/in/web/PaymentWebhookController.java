package com.projects.adapter.in.web;

import com.projects.application.port.in.billing.ProcessPaymentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final ProcessPaymentUseCase processPaymentUseCase;

    @SuppressWarnings("unchecked")
    @PostMapping("/webhook")
    public Mono<ResponseEntity<String>> handleStripeWebhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            @RequestBody Map<String, Object> payload) {

        log.info("Received Stripe webhook, type={}", payload.get("type"));

        // Dans une vraie implémentation, il faut valider la signature Stripe :
        // Event event = Webhook.constructEvent(payloadString, signature, endpointSecret);

        String type = (String) payload.get("type");
        if ("checkout.session.completed".equals(type)) {
            Map<String, Object> data   = (Map<String, Object>) payload.get("data");
            Map<String, Object> object = (Map<String, Object>) data.get("object");

            // organisationId passé dans client_reference_id, planId dans metadata.plan
            String organizationId = (String) object.get("client_reference_id");
            String paymentId      = (String) object.get("id");

            Map<String, String> metadata = (Map<String, String>) object.get("metadata");
            String planId = (metadata != null && metadata.containsKey("plan"))
                    ? metadata.get("plan") : "PRO";

            if (organizationId != null) {
                return processPaymentUseCase
                        .processSuccessfulPayment(organizationId, planId, paymentId)
                        .thenReturn(ResponseEntity.ok("Webhook traité avec succès"))
                        .onErrorResume(e -> {
                            log.error("Erreur traitement webhook paiement : ", e);
                            return Mono.just(ResponseEntity.status(500)
                                    .body("Erreur lors du traitement du webhook"));
                        });
            }
        }

        return Mono.just(ResponseEntity.ok("Webhook ignoré ou type non géré"));
    }
}
