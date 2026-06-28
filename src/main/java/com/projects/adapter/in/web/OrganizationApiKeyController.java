package com.projects.adapter.in.web;

import com.projects.adapter.in.web.dto.ApiKeyGenerateRequest;
import com.projects.adapter.in.web.dto.ApiKeyResponse;
import com.projects.application.port.in.ManageApiKeyUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/organizations/{organizationId}/api-keys")
@RequiredArgsConstructor
@Tag(name = "API Keys", description = "Gestion des clés d'API des organisations")
public class OrganizationApiKeyController {

    private final ManageApiKeyUseCase manageApiKeyUseCase;

    @PostMapping
    @Operation(summary = "Générer une nouvelle clé API pour l'organisation")
    public Mono<ResponseEntity<ApiKeyResponse>> generateApiKey(
            @PathVariable UUID organizationId,
            @RequestBody(required = false) ApiKeyGenerateRequest request) {
        
        String label = (request != null) ? request.getLabel() : "Default Key";
        return manageApiKeyUseCase.generateApiKey(organizationId, label)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/revoke")
    @Operation(summary = "Révoquer la clé API de l'organisation")
    public Mono<ResponseEntity<Void>> revokeApiKey(@PathVariable UUID organizationId) {
        return manageApiKeyUseCase.revokeApiKey(organizationId)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
}
