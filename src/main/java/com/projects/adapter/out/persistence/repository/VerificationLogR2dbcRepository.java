package com.projects.adapter.out.persistence.repository;

import com.projects.adapter.out.persistence.entity.VerificationLogEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Spring Data R2DBC repository for VerificationLogEntity.
 */
public interface VerificationLogR2dbcRepository extends ReactiveCrudRepository<VerificationLogEntity, Long> {
    
    @Query("SELECT COUNT(*) FROM verification_logs WHERE platform_id = :platformId AND date >= :date")
    Mono<Long> countByPlatformIdAndDateAfter(UUID platformId, LocalDateTime date);

    @Query("SELECT * FROM verification_logs WHERE platform_id = :platformId ORDER BY date DESC")
    Flux<VerificationLogEntity> findByPlatformId(UUID platformId);
}
