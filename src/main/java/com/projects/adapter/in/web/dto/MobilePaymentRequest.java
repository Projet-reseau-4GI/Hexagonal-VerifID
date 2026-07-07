package com.projects.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO pour initier un paiement via Mobile Money.
 */
@Data
public class MobilePaymentRequest {

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Format de numéro invalide")
    private String phoneNumber;

    @NotBlank(message = "L'opérateur est obligatoire (ex: MTN, ORANGE, WAVE)")
    private String operator;

    @NotBlank(message = "Le plan d'abonnement est obligatoire (ex: STARTER, PRO)")
    private String planId;
}
