package com.projects.application.port.in.billing;

import com.projects.adapter.in.web.dto.MobilePaymentRequest;
import com.projects.adapter.in.web.dto.MobilePaymentResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProcessPaymentUseCase {

    /** Processus de retour classique (ex: Stripe ou webhook validé) */
    Mono<Void> processSuccessfulPayment(String organizationId, String planId, String paymentId);

    /** Initie un paiement via une API de Mobile Money (MTN, Orange, etc.) */
    Mono<MobilePaymentResponse> initiateMobilePayment(UUID organizationId, MobilePaymentRequest request);
}
