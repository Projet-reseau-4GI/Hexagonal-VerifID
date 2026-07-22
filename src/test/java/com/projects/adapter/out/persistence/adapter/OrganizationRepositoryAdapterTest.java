package com.projects.adapter.out.persistence.adapter;

import com.projects.application.port.out.OrganizationRepositoryPort;
import com.projects.domain.model.Organization;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tests permettant de remplir la table des organisations localement 
 * (sans appel au Kernel). Le plan tarifaire est géré par VerifID.
 */
@SpringBootTest
class OrganizationRepositoryAdapterTest {

    @Autowired
    private OrganizationRepositoryPort organizationRepository;

    @Test
    void shouldInsertOrganizationLocallyWithoutKernel() {
        // Étant donné une nouvelle organisation locale VerifID
        Organization mockOrg = Organization.builder()
                .id(UUID.randomUUID()) // Normalement généré par le Kernel, ici on mock pour le test
                .email("tadidajalil01@gmail.com")
                .developerName("Polytech")
                .organizationName("Polytech")
                // Le plan tarifaire est géré par VerifID et non par le Kernel
                .plan("FREE")
                .createdAt(LocalDateTime.now())
                .lastSyncedAt(LocalDateTime.now())
                .apiKeyActive(true)
                .build();

        // Quand on sauvegarde directement dans notre BD
        Mono<Organization> savedOrgMono = organizationRepository.save(mockOrg);

        // Alors l'organisation est enregistrée
        StepVerifier.create(savedOrgMono)
                .expectNextMatches(savedOrg -> {
                    return savedOrg.getId().equals(mockOrg.getId()) &&
                           "tadidajalil01@gmail.com".equals(savedOrg.getEmail()) &&
                           "FREE".equals(savedOrg.getPlan());
                })
                .verifyComplete();
    }
}
