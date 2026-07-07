package com.projects.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for selecting an organization in the twin authentication process.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête pour sélectionner une organisation après l'authentification Kernel")
public class SelectOrgRequest {

    @NotBlank(message = "Le token Kernel est requis")
    @Schema(description = "Jeton d'accès Kernel", example = "eyJhbG...")
    private String kernelToken;

    @Schema(description = "ID de l'organisation sélectionnée", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID organizationId;
}
