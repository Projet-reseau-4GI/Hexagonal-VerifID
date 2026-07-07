package com.projects.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for initiating the twin authentication process via Kernel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête de connexion initiale via le KSM Kernel")
public class KernelLoginRequest {

    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    @Schema(description = "Email du propriétaire", example = "owner@organization.com")
    private String email;

    @NotBlank(message = "Le mot de passe est requis")
    @Schema(description = "Mot de passe", example = "secret")
    private String password;
}
