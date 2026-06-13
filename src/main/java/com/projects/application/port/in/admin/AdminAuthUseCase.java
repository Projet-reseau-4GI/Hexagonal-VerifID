package com.projects.application.port.in.admin;

import com.projects.adapter.in.web.dto.admin.AdminLoginRequest;
import com.projects.adapter.in.web.dto.admin.AdminRegisterRequest;
import com.projects.adapter.in.web.dto.admin.AdminVerifyOtpRequest;
import com.projects.adapter.in.web.dto.admin.AdminAuthResponse;
import reactor.core.publisher.Mono;

public interface AdminAuthUseCase {
    Mono<Void> registerAdmin(AdminRegisterRequest request);
    Mono<Void> loginAdmin(AdminLoginRequest request);
    Mono<AdminAuthResponse> verifyOtp(AdminVerifyOtpRequest request);
}
