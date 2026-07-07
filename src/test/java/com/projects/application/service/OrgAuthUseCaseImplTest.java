package com.projects.application.service;

import com.projects.adapter.in.web.dto.OrgRegisterRequest;
import com.projects.application.port.out.EmailServicePort;
import com.projects.application.port.out.OrganizationRepositoryPort;
import com.projects.application.service.admin.LocalJwtService;
import com.projects.domain.model.Organization;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrgAuthUseCaseImplTest {

    @Test
    void registerShouldSucceedWhenEmailSendingFails() {
        OrganizationRepositoryPort organizationRepository = mock(OrganizationRepositoryPort.class);
        EmailServicePort emailService = mock(EmailServicePort.class);
        LocalJwtService jwtService = mock(LocalJwtService.class);
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        com.projects.application.port.out.KernelAuthPort kernelAuthPort = mock(com.projects.application.port.out.KernelAuthPort.class);

        OrgAuthUseCaseImpl useCase = new OrgAuthUseCaseImpl(organizationRepository, emailService, jwtService, passwordEncoder, kernelAuthPort);

        OrgRegisterRequest request = new OrgRegisterRequest();
        request.setOrganizationName("Test Org");
        request.setEmail("test@example.com");
        request.setPassword("Password123");
        request.setDisplayName("Test Org");

        when(organizationRepository.existsByEmail("test@example.com")).thenReturn(Mono.just(false));
        when(passwordEncoder.encode("Password123")).thenReturn("encoded-password");
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization org = invocation.getArgument(0);
            org.setId(UUID.randomUUID());
            org.setCreatedAt(LocalDateTime.now());
            return Mono.just(org);
        });
        when(emailService.sendOtp(anyString(), anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("401 Unauthorized")));

        StepVerifier.create(useCase.register(request))
                .verifyComplete();

        verify(organizationRepository).existsByEmail("test@example.com");
        verify(organizationRepository).save(any(Organization.class));
        verify(emailService).sendOtp(eq("test@example.com"), anyString(), eq("Test Org"));
    }
}
