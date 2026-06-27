package com.projects.application.service.admin;

import com.projects.adapter.in.web.dto.admin.AdminAuthResponse;
import com.projects.adapter.in.web.dto.admin.AdminLoginRequest;
import com.projects.adapter.in.web.dto.admin.AdminRegisterRequest;
import com.projects.adapter.in.web.dto.admin.AdminVerifyOtpRequest;
import com.projects.application.port.in.admin.AdminAuthUseCase;
import com.projects.application.port.out.AdminRepositoryPort;
import com.projects.application.port.out.EmailServicePort;
import com.projects.domain.model.Admin;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AdminAuthUseCaseImpl implements AdminAuthUseCase {

    private final AdminRepositoryPort adminRepository;
    private final EmailServicePort emailService;
    private final PasswordEncoder passwordEncoder;
    private final LocalJwtService jwtService;
    private final Random random = new Random();

    @Override
    public Mono<Void> registerAdmin(AdminRegisterRequest request) {
        return adminRepository.findByEmail(request.getEmail())
                .flatMap(existing -> Mono.<Void>error(new RuntimeException("Un administrateur avec cet email existe déjà")))
                .switchIfEmpty(Mono.defer(() -> {
                    String otp = generateOtp();
                    Admin admin = Admin.builder()
                            .name(request.getName())
                            .email(request.getEmail())
                            .passwordHash(passwordEncoder.encode(request.getPassword()))
                            .role("SUPER_ADMIN")
                            .otpCode(otp)
                            .otpExpiry(LocalDateTime.now().plusMinutes(15))
                            .isVerified(false)
                            .createdAt(LocalDateTime.now())
                            .build();

                    return adminRepository.save(admin)
                            .flatMap(saved -> emailService.sendOtp(saved.getEmail(), otp, "VerifID Admin"))
                            .then();
                }));
    }

    @Override
    public Mono<Void> loginAdmin(AdminLoginRequest request) {
        return adminRepository.findByEmail(request.getEmail())
                .switchIfEmpty(Mono.error(new RuntimeException("Identifiants incorrects")))
                .flatMap(admin -> {
                    if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
                        return Mono.error(new RuntimeException("Identifiants incorrects"));
                    }
                    String otp = generateOtp();
                    admin.setOtpCode(otp);
                    admin.setOtpExpiry(LocalDateTime.now().plusMinutes(15));
                    
                    return adminRepository.save(admin)
                            .flatMap(saved -> emailService.sendOtp(saved.getEmail(), otp, "VerifID Admin"))
                            .then();
                });
    }

    @Override
    public Mono<AdminAuthResponse> verifyOtp(AdminVerifyOtpRequest request) {
        return adminRepository.findByEmail(request.getEmail())
                .switchIfEmpty(Mono.error(new RuntimeException("Administrateur introuvable")))
                .flatMap(admin -> {
                    if (admin.getOtpCode() == null || !admin.getOtpCode().equals(request.getOtpCode())) {
                        return Mono.error(new RuntimeException("Code OTP invalide"));
                    }
                    if (admin.getOtpExpiry().isBefore(LocalDateTime.now())) {
                        return Mono.error(new RuntimeException("Code OTP expiré"));
                    }

                    admin.setVerified(true);
                    admin.setOtpCode(null);
                    admin.setOtpExpiry(null);

                    return adminRepository.save(admin)
                            .map(saved -> {
                                String token = jwtService.generateToken(saved.getEmail(), saved.getRole());
                                return AdminAuthResponse.builder()
                                        .token(token)
                                        .email(saved.getEmail())
                                        .role(saved.getRole())
                                        .build();
                            });
                });
    }

    private String generateOtp() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}
