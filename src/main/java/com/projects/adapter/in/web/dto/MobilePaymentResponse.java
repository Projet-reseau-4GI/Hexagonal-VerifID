package com.projects.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse suite à l'initiation d'un paiement Mobile Money.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobilePaymentResponse {

    private String transactionId;
    private String status; // Ex: PENDING, PROMPT_SENT
    private String message; // Ex: "Veuillez valider le paiement sur votre téléphone"
    private String paymentUrl; // Optionnel (si l'utilisateur doit être redirigé)

}
