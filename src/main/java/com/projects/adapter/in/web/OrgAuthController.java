package com.projects.adapter.in.web;

import com.projects.adapter.in.web.dto.OrgAuthResponse;
import com.projects.adapter.in.web.dto.OrgInitiateAuthRequest;
import com.projects.adapter.in.web.dto.OrgVerifyOtpRequest;
import com.projects.application.port.in.OrgAuthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/org/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Organization Auth", description = "Authentification des organisations via Kernel Core")
public class OrgAuthController {

    private final OrgAuthUseCase orgAuthUseCase;

    @PostMapping("/initiate")
    @Operation(summary = "Étape 1 : Demande l'envoi d'un code OTP par email via le Kernel")
    public Mono<ResponseEntity<String>> initiateAuth(@Valid @RequestBody OrgInitiateAuthRequest request) {
        return orgAuthUseCase.initiateAuth(request)
                .then(Mono.just(ResponseEntity.ok("Si l'organisation existe, un code OTP a été envoyé à l'adresse email.")));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Étape 2 : Vérifie le code OTP et retourne le JWT + informations de l'organisation")
    public Mono<ResponseEntity<OrgAuthResponse>> verifyOtp(@Valid @RequestBody OrgVerifyOtpRequest request) {
        return orgAuthUseCase.completeAuth(request)
                .map(ResponseEntity::ok);
    }
}
