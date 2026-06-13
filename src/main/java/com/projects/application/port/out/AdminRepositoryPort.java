package com.projects.application.port.out;

import com.projects.domain.model.Admin;
import reactor.core.publisher.Mono;

public interface AdminRepositoryPort {
    Mono<Admin> save(Admin admin);
    Mono<Admin> findByEmail(String email);
    Mono<Admin> findById(Long id);
}
