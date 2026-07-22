package com.projects.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Requête d'inscription d'une nouvelle organisation (mode autonome, sans
 * Kernel).
 */
@Data
public class OrgRegisterRequest {

    @NotBlank(message = "Le nom du développeur est obligatoire")
    private String devName;

    /** Nom de l'organisation (optionnel) */
    private String organizationName;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;
}
