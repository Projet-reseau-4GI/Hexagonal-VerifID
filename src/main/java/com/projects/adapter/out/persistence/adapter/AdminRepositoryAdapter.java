package com.projects.adapter.out.persistence.adapter;

import com.projects.adapter.out.persistence.mapper.AdminMapper;
import com.projects.adapter.out.persistence.repository.AdminR2dbcRepository;
import com.projects.application.port.out.AdminRepositoryPort;
import com.projects.domain.model.Admin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AdminRepositoryAdapter implements AdminRepositoryPort {

    private final AdminR2dbcRepository repository;
    private final AdminMapper mapper;

    @Override
    public Mono<Admin> save(Admin admin) {
        return repository.save(mapper.toEntity(admin))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Admin> findByEmail(String email) {
        return repository.findByEmail(email)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Admin> findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }
}
