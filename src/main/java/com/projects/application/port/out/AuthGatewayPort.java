package com.projects.application.port.out;

import reactor.core.publisher.Mono;
import java.util.Map;
import java.util.UUID;

/**
 * Port sortant pour la communication avec le module d'authentification (auth_core).
 */
public interface AuthGatewayPort {
    Mono<ClientAppRegistrationDTO> registerClientApplication(String adminBearerToken, UUID tenantId);
    Mono<AuthLoginDTO> loginUser(UUID tenantId, String principal, String password);
    Mono<Map<String, Object>> discoverLoginContexts(String email);
    
    record ClientAppRegistrationDTO(String clientId, String plainSecret, String status) {}
    record AuthLoginDTO(String accessToken, String refreshToken, UUID userId, String email, String status) {}
}
