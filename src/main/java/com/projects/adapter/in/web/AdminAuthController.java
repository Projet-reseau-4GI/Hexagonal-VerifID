package com.projects.adapter.in.web;

import com.projects.adapter.in.web.dto.admin.AdminAuthResponse;
import com.projects.adapter.in.web.dto.admin.AdminLoginRequest;
import com.projects.adapter.in.web.dto.admin.AdminRegisterRequest;
import com.projects.adapter.in.web.dto.admin.AdminVerifyOtpRequest;
import com.projects.application.port.in.admin.AdminAuthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Admin Auth", description = "Authentification des Super Admins pour l'accès aux statistiques")
public class AdminAuthController {

    private final AdminAuthUseCase adminAuthUseCase;

    @PostMapping("/register")
    @Operation(summary = "Inscription d'un administrateur (Envoie un OTP)")
    public Mono<ResponseEntity<String>> register(@Valid @RequestBody AdminRegisterRequest request) {
        return adminAuthUseCase.registerAdmin(request)
                .then(Mono.just(ResponseEntity.ok("Inscription réussie, un code OTP a été envoyé à votre email.")));
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion d'un administrateur (Envoie un OTP)")
    public Mono<ResponseEntity<String>> login(@Valid @RequestBody AdminLoginRequest request) {
        return adminAuthUseCase.loginAdmin(request)
                .then(Mono.just(ResponseEntity.ok("Identifiants valides, un code OTP a été envoyé à votre email.")));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Vérification du code OTP (Retourne un JWT)")
    public Mono<ResponseEntity<AdminAuthResponse>> verifyOtp(@Valid @RequestBody AdminVerifyOtpRequest request) {
        return adminAuthUseCase.verifyOtp(request)
                .map(ResponseEntity::ok);
    }
}
