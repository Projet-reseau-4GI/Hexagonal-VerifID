package com.projects.adapter.out.persistence.repository;

import com.projects.adapter.out.persistence.entity.AdminEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface AdminR2dbcRepository extends ReactiveCrudRepository<AdminEntity, Long> {
    Mono<AdminEntity> findByEmail(String email);
}
