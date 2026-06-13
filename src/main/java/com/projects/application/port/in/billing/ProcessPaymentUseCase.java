package com.projects.application.port.in.billing;

import reactor.core.publisher.Mono;

public interface ProcessPaymentUseCase {
    Mono<Void> processSuccessfulPayment(String organizationId, String planId, String paymentId);
}
