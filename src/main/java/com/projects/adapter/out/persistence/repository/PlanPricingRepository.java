package com.projects.adapter.out.persistence.repository;

import com.projects.adapter.out.persistence.entity.PlanPricingEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanPricingRepository extends R2dbcRepository<PlanPricingEntity, String> {
}
