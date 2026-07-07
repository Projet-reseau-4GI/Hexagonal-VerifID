package com.projects.adapter.out.kernel;

import com.projects.adapter.out.kernel.dto.KernelListOrgApiResponse;
import com.projects.adapter.out.kernel.dto.KernelOrgResponse;
import com.projects.adapter.out.kernel.dto.KernelTokenResponse;
import com.projects.application.port.out.KernelAuthPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Adapter for integrating with the KSM Kernel Authentication Core.
 *
 * Implements the {@link KernelAuthPort} to handle external API calls to the
 * centralized Kernel service for login and fetching organizations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KernelAuthAdapter implements KernelAuthPort {

    private final WebClient.Builder webClientBuilder;

    @Value("${kernel.base-url:http://kernel-core.yowyob.com}")
    private String kernelBaseUrl;

    @Value("${kernel.client-id:verifid}")
    private String clientId;

    @Value("${kernel.api-key:fbvW-uSPKssHgT4hyY48wXz6Np3D2T9CX1gnSEHFQQA}")
    private String clientSecret;

    @Override
    public Mono<KernelTokenResponse> login(String email, String password) {
        log.info("[KernelAuthAdapter] Attempting Kernel login for user: {}", email);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("username", email);
        formData.add("password", password);

        WebClient client = webClientBuilder.baseUrl(kernelBaseUrl).build();

        return client.post()
                .uri("/oauth2/token")
                .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(KernelTokenResponse.class)
                .doOnSuccess(res -> log.info("[KernelAuthAdapter] Kernel login successful for user: {}", email))
                .doOnError(err -> log.error("[KernelAuthAdapter] Kernel login failed for user: {}", email, err));
    }

    @Override
    public Mono<List<KernelOrgResponse>> getMyOrganizations(String kernelToken) {
        log.info("[KernelAuthAdapter] Fetching organizations for the authenticated user");

        WebClient client = webClientBuilder.baseUrl(kernelBaseUrl).build();

        return client.get()
                .uri("/api/organizations/my")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + kernelToken)
                .retrieve()
                .bodyToMono(KernelListOrgApiResponse.class)
                .map(response -> {
                    if (Boolean.TRUE.equals(response.getSuccess())) {
                        return response.getData();
                    }
                    throw new RuntimeException("Kernel API returned unsuccessful response: " + response.getMessage());
                })
                .doOnSuccess(orgs -> log.info("[KernelAuthAdapter] Successfully fetched {} organizations", orgs.size()))
                .doOnError(err -> log.error("[KernelAuthAdapter] Failed to fetch organizations from Kernel", err));
    }
}
