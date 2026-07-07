package com.projects.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Requête de vérification de l'email via code OTP (étape d'inscription).
 */
@Data
public class OrgVerifyEmailRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank(message = "Le code OTP est obligatoire")
    private String otpCode;
}
