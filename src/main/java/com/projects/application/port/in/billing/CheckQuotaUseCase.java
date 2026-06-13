package com.projects.application.port.in.billing;

import reactor.core.publisher.Mono;

public interface CheckQuotaUseCase {
    Mono<Boolean> isQuotaAvailable(String organizationId);
}
