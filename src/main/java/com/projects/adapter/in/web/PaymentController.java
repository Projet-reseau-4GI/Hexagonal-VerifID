package com.projects.adapter.in.web;

import com.projects.adapter.in.web.dto.MobilePaymentRequest;
import com.projects.adapter.in.web.dto.MobilePaymentResponse;
import com.projects.application.port.in.billing.ProcessPaymentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Paiements", description = "Endpoints mobiles et webhooks de paiement")
@Slf4j
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;

    @PostMapping("/mobile/initiate")
    @Operation(summary = "Initier un paiement Mobile Money")
    public Mono<ResponseEntity<MobilePaymentResponse>> initiatePayment(
            @Valid @RequestBody MobilePaymentRequest request,
            @RequestHeader("X-Organization-Id") String organizationId) {

        return processPaymentUseCase.initiateMobilePayment(UUID.fromString(organizationId), request)
                .map(ResponseEntity::ok);
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/webhook")
    @Operation(summary = "Traiter un webhook de paiement")
    public Mono<ResponseEntity<String>> handleWebhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            @RequestBody Map<String, Object> payload) {

        log.info("Received Stripe webhook, type={}", payload.get("type"));

        String type = (String) payload.get("type");
        if (!"checkout.session.completed".equals(type)) {
            return Mono.just(ResponseEntity.ok("Webhook ignoré ou type non géré"));
        }

        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        if (data == null) {
            return Mono.just(ResponseEntity.badRequest().body("Payload webhook invalide"));
        }

        Map<String, Object> object = (Map<String, Object>) data.get("object");
        if (object == null) {
            return Mono.just(ResponseEntity.badRequest().body("Payload webhook invalide"));
        }

        String organizationId = (String) object.get("client_reference_id");
        String paymentId = (String) object.get("id");
        Map<String, String> metadata = (Map<String, String>) object.get("metadata");
        String planId = metadata != null && metadata.containsKey("plan") ? metadata.get("plan") : "PRO";

        if (organizationId == null || organizationId.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().body("client_reference_id manquant"));
        }

        return processPaymentUseCase.processSuccessfulPayment(organizationId, planId, paymentId)
                .thenReturn(ResponseEntity.ok("Webhook traité avec succès"))
                .onErrorResume(e -> {
                    log.error("Erreur traitement webhook paiement", e);
                    return Mono.just(ResponseEntity.status(500).body("Erreur lors du traitement du webhook"));
                });
    }
}
