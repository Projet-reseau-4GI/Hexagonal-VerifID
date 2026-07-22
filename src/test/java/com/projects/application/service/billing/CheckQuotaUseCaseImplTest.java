package com.projects.application.service.billing;

import com.projects.application.port.out.OrganizationRepositoryPort;
import com.projects.application.port.out.QuotaRedisPort;
import com.projects.domain.model.Organization;
import com.projects.domain.model.Plan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour CheckQuotaUseCaseImpl.
 * Vérifie les limites par plan : FREE=0, PREMIUM=100, MAX=1000.
 */
class CheckQuotaUseCaseImplTest {

    private OrganizationRepositoryPort orgRepo;
    private QuotaRedisPort quotaRedis;
    private ApplicationEventPublisher publisher;
    private CheckQuotaUseCaseImpl useCase;

    private static final String ORG_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        orgRepo = mock(OrganizationRepositoryPort.class);
        quotaRedis = mock(QuotaRedisPort.class);
        publisher = mock(ApplicationEventPublisher.class);
        useCase = new CheckQuotaUseCaseImpl(orgRepo, quotaRedis, publisher);
    }

    private Organization orgWithPlan(String plan) {
        Organization org = Organization.builder()
                .id(UUID.fromString(ORG_ID))
                .email("test@verifid.app")
                .name("TestOrg")
                .plan(plan)
                .status("ACTIVE")
                .build();
        return org;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Plan FREE — toujours bloqué
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Plan FREE")
    class FreePlanTests {

        @Test
        @DisplayName("isQuotaAvailable retourne false pour plan FREE (0 requêtes autorisées)")
        void freePlanAlwaysBlocked() {
            when(orgRepo.findById(any(UUID.class))).thenReturn(Mono.just(orgWithPlan("FREE")));

            StepVerifier.create(useCase.isQuotaAvailable(ORG_ID))
                    .expectNext(false)
                    .verifyComplete();

            // Redis ne doit jamais être consulté pour le plan FREE
            verifyNoInteractions(quotaRedis);
        }

        @Test
        @DisplayName("getQuotaStatus retourne exceeded=true et limit=0 pour plan FREE")
        void freePlanQuotaStatusExceeded() {
            when(orgRepo.findById(any(UUID.class))).thenReturn(Mono.just(orgWithPlan("FREE")));

            StepVerifier.create(useCase.getQuotaStatus(ORG_ID))
                    .assertNext(qs -> {
                        assert qs.exceeded() : "FREE plan doit être exceeded";
                        assert qs.limit() == 0 : "FREE plan limit doit être 0";
                        assert qs.consumed() == 0 : "FREE plan consumed doit être 0";
                        assert "FREE".equals(qs.plan()) : "Plan doit être FREE";
                    })
                    .verifyComplete();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Plan PREMIUM — 100 requêtes/jour
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Plan PREMIUM")
    class PremiumPlanTests {

        @BeforeEach
        void setUpPremium() {
            when(orgRepo.findById(any(UUID.class))).thenReturn(Mono.just(orgWithPlan("PREMIUM")));
        }

        @Test
        @DisplayName("99 requêtes < 100 : disponible")
        void at99QuotaAvailable() {
            when(quotaRedis.getCurrentCount(eq(ORG_ID), any(LocalDate.class))).thenReturn(Mono.just(99L));
            StepVerifier.create(useCase.isQuotaAvailable(ORG_ID))
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("100 requêtes == 100 : quota atteint, bloqué")
        void at100QuotaBlocked() {
            when(quotaRedis.getCurrentCount(eq(ORG_ID), any(LocalDate.class))).thenReturn(Mono.just(100L));
            StepVerifier.create(useCase.isQuotaAvailable(ORG_ID))
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("101 requêtes > 100 : quota dépassé, bloqué")
        void at101QuotaBlocked() {
            when(quotaRedis.getCurrentCount(eq(ORG_ID), any(LocalDate.class))).thenReturn(Mono.just(101L));
            StepVerifier.create(useCase.isQuotaAvailable(ORG_ID))
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("getQuotaStatus retourne limit=100 pour PREMIUM")
        void premiumLimitIs100() {
            when(quotaRedis.getCurrentCount(eq(ORG_ID), any(LocalDate.class))).thenReturn(Mono.just(50L));
            StepVerifier.create(useCase.getQuotaStatus(ORG_ID))
                    .assertNext(qs -> {
                        assert qs.limit() == 100L : "PREMIUM limit doit être 100";
                        assert qs.consumed() == 50L;
                        assert !qs.exceeded();
                    })
                    .verifyComplete();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Plan MAX — 1000 requêtes/jour
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Plan MAX")
    class MaxPlanTests {

        @BeforeEach
        void setUpMax() {
            when(orgRepo.findById(any(UUID.class))).thenReturn(Mono.just(orgWithPlan("MAX")));
        }

        @Test
        @DisplayName("999 requêtes < 1000 : disponible")
        void at999QuotaAvailable() {
            when(quotaRedis.getCurrentCount(eq(ORG_ID), any(LocalDate.class))).thenReturn(Mono.just(999L));
            StepVerifier.create(useCase.isQuotaAvailable(ORG_ID))
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("1000 requêtes == 1000 : quota atteint, bloqué")
        void at1000QuotaBlocked() {
            when(quotaRedis.getCurrentCount(eq(ORG_ID), any(LocalDate.class))).thenReturn(Mono.just(1000L));
            StepVerifier.create(useCase.isQuotaAvailable(ORG_ID))
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("1001 requêtes > 1000 : quota dépassé, bloqué")
        void at1001QuotaBlocked() {
            when(quotaRedis.getCurrentCount(eq(ORG_ID), any(LocalDate.class))).thenReturn(Mono.just(1001L));
            StepVerifier.create(useCase.isQuotaAvailable(ORG_ID))
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("getQuotaStatus retourne limit=1000 pour MAX")
        void maxLimitIs1000() {
            when(quotaRedis.getCurrentCount(eq(ORG_ID), any(LocalDate.class))).thenReturn(Mono.just(500L));
            StepVerifier.create(useCase.getQuotaStatus(ORG_ID))
                    .assertNext(qs -> {
                        assert qs.limit() == 1000L : "MAX limit doit être 1000";
                        assert qs.consumed() == 500L;
                        assert !qs.exceeded();
                    })
                    .verifyComplete();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cas d'erreur
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isQuotaAvailable retourne false si organisation non trouvée")
    void orgNotFoundReturnsFalse() {
        when(orgRepo.findById(any(UUID.class))).thenReturn(Mono.empty());
        StepVerifier.create(useCase.isQuotaAvailable(ORG_ID))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("isQuotaAvailable retourne false si UUID invalide")
    void invalidUuidReturnsFalse() {
        StepVerifier.create(useCase.isQuotaAvailable("not-a-uuid"))
                .expectNext(false)
                .verifyComplete();
    }
}
