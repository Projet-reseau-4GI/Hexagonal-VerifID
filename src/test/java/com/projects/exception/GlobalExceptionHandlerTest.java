package com.projects.exception;

import com.projects.adapter.in.web.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.RequestPath;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    @Test
    void duplicateEmailShouldReturnConflict() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        org.springframework.http.server.reactive.ServerHttpRequest request = mock(org.springframework.http.server.reactive.ServerHttpRequest.class);
        RequestPath path = mock(RequestPath.class);

        when(exchange.getRequest()).thenReturn(request);
        when(request.getPath()).thenReturn(path);
        when(path.value()).thenReturn("/api/org/auth/register");

        Mono<ResponseEntity<ErrorResponse>> result = handler.handleIllegalArgumentException(
                new IllegalArgumentException("Un compte existe déjà pour cet email : test@example.com"),
                exchange);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assert response.getStatusCode().value() == 409;
                    assert response.getBody() != null;
                    assert response.getBody().getMessage().contains("Un compte existe déjà");
                })
                .verifyComplete();
    }
}
