package com.projects.adapter.out.persistence.repository;

import com.projects.adapter.out.persistence.entity.VerificationLogEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

/**
 * Spring Data R2DBC repository for VerificationLogEntity.
 */
public interface VerificationLogR2dbcRepository extends ReactiveCrudRepository<VerificationLogEntity, Long> {
    
    @Query("SELECT COUNT(*) FROM verification_logs WHERE platform_id = :platformId AND date >= :date")
    Mono<Long> countByPlatformIdAndDateAfter(String platformId, LocalDateTime date);

    @Query("SELECT * FROM verification_logs WHERE platform_id = :platformId ORDER BY date DESC")
    Flux<VerificationLogEntity> findByPlatformId(String platformId);

    @Query("SELECT * FROM verification_logs WHERE api_key_id = :apiKeyId ORDER BY date DESC")
    Flux<VerificationLogEntity> findByApiKeyId(Long apiKeyId);
}
