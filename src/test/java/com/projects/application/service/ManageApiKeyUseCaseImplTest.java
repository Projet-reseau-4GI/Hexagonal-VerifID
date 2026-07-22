package com.projects.application.service;

import com.projects.adapter.out.security.SecurityUtils;
import com.projects.application.port.out.EmailServicePort;
import com.projects.application.port.out.OrganizationRepositoryPort;
import com.projects.domain.model.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour ManageApiKeyUseCaseImpl.
 * Vérifie la génération, rotation et révocation des API keys.
 */
class ManageApiKeyUseCaseImplTest {

    private OrganizationRepositoryPort orgRepo;
    private EmailServicePort emailService;
    private ReactiveStringRedisTemplate redisTemplate;
    private ManageApiKeyUseCaseImpl useCase;

    private final UUID ORG_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        orgRepo = mock(OrganizationRepositoryPort.class);
        emailService = mock(EmailServicePort.class);
        redisTemplate = mock(ReactiveStringRedisTemplate.class);

        when(emailService.sendApiKeyCreatedNotification(anyString(), anyString())).thenReturn(Mono.empty());
        when(emailService.sendApiKeyDeletedNotification(anyString(), anyString())).thenReturn(Mono.empty());
        when(redisTemplate.delete(anyString())).thenReturn(Mono.just(1L));

        useCase = new ManageApiKeyUseCaseImpl(orgRepo, emailService, redisTemplate);
    }

    private Organization activeOrg() {
        return Organization.builder()
                .id(ORG_ID)
                .email("org@verifid.app")
                .developerName("TestOrg")
                .organizationName("Test Organisation")
                .plan("PREMIUM")
                .status("ACTIVE")
                .apiKeyActive(true)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Génération
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateApiKey retourne une clé en clair commençant par 'vf_id_'")
    void generateApiKeyReturnsClearKey() {
        Organization org = activeOrg();
        when(orgRepo.findById(ORG_ID)).thenReturn(Mono.just(org));
        when(orgRepo.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.generateApiKey(ORG_ID, "test-label"))
                .assertNext(response -> {
                    assertThat(response.getApiKey()).startsWith("vf_id_");
                    assertThat(response.getApiKey()).hasSizeGreaterThan(20);
                    assertThat(response.getActive()).isTrue();
                    assertThat(response.getLabel()).isEqualTo("test-label");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("generateApiKey stocke le hash SHA-256 (pas la clé brute) en base")
    void generateApiKeyStoresHashNotRawKey() {
        Organization org = activeOrg();
        AtomicReference<String> storedHash = new AtomicReference<>();
        AtomicReference<String> returnedRawKey = new AtomicReference<>();

        when(orgRepo.findById(ORG_ID)).thenReturn(Mono.just(org));
        when(orgRepo.save(any())).thenAnswer(inv -> {
            Organization saved = inv.getArgument(0);
            storedHash.set(saved.getApiKeyHash());
            return Mono.just(saved);
        });

        StepVerifier.create(useCase.generateApiKey(ORG_ID, "hash-test"))
                .assertNext(response -> returnedRawKey.set(response.getApiKey()))
                .verifyComplete();

        // Le hash stocké doit correspondre au hash SHA-256 de la clé brute
        String expectedHash = SecurityUtils.hashApiKey(returnedRawKey.get());
        assertThat(storedHash.get()).isEqualTo(expectedHash);
        assertThat(storedHash.get()).isNotEqualTo(returnedRawKey.get());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rotation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("rotateApiKey retourne une nouvelle clé différente de l'ancienne")
    void rotateApiKeyReturnsDifferentKey() {
        Organization org = activeOrg();
        org.setApiKeyHash(SecurityUtils.hashApiKey("vf_id_oldkey123456"));
        org.setApiKeyLabel("my-key");

        AtomicReference<String> newKey = new AtomicReference<>();
        when(orgRepo.findById(ORG_ID)).thenReturn(Mono.just(org));
        when(orgRepo.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.rotateApiKey(ORG_ID))
                .assertNext(response -> {
                    newKey.set(response.getApiKey());
                    assertThat(response.getApiKey()).startsWith("vf_id_");
                    assertThat(response.getApiKey()).isNotEqualTo("vf_id_oldkey123456");
                    assertThat(response.getActive()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("rotateApiKey invalide l'ancienne clé dans Redis")
    void rotateApiKeyEvictsOldKeyFromRedis() {
        String oldHash = SecurityUtils.hashApiKey("vf_id_oldkey_to_evict");
        Organization org = activeOrg();
        org.setApiKeyHash(oldHash);

        when(orgRepo.findById(ORG_ID)).thenReturn(Mono.just(org));
        when(orgRepo.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.rotateApiKey(ORG_ID))
                .assertNext(response -> assertThat(response.getApiKey()).startsWith("vf_id_"))
                .verifyComplete();

        verify(redisTemplate).delete("apikey:" + oldHash);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Révocation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("revokeApiKey positionne api_key_active=false en base")
    void revokeApiKeySetsActiveToFalse() {
        Organization org = activeOrg();
        org.setApiKeyHash(SecurityUtils.hashApiKey("vf_id_to_revoke"));
        AtomicReference<Boolean> savedActive = new AtomicReference<>();

        when(orgRepo.findById(ORG_ID)).thenReturn(Mono.just(org));
        when(orgRepo.save(any())).thenAnswer(inv -> {
            Organization saved = inv.getArgument(0);
            savedActive.set(saved.getApiKeyActive());
            return Mono.just(saved);
        });

        StepVerifier.create(useCase.revokeApiKey(ORG_ID))
                .verifyComplete();

        assertThat(savedActive.get()).isFalse();
    }

    @Test
    @DisplayName("revokeApiKey supprime la clé du cache Redis")
    void revokeApiKeyEvictsFromRedis() {
        String hash = SecurityUtils.hashApiKey("vf_id_revoke_redis");
        Organization org = activeOrg();
        org.setApiKeyHash(hash);

        when(orgRepo.findById(ORG_ID)).thenReturn(Mono.just(org));
        when(orgRepo.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.revokeApiKey(ORG_ID))
                .verifyComplete();

        verify(redisTemplate).delete("apikey:" + hash);
    }
}
