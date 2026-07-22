package com.projects.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("plan_pricing")
public class PlanPricingEntity {

    @Id
    @Column("plan_id")
    private String planId;

    @Column("price")
    private Double price;

    @Column("currency")
    private String currency;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
