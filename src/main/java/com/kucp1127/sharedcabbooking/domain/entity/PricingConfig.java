package com.kucp1127.sharedcabbooking.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "pricing_configs", indexes = {
    @Index(name = "idx_pricing_config_active", columnList = "is_active"),
    @Index(name = "idx_pricing_config_type", columnList = "config_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingConfig extends BaseEntity {

    @Column(name = "config_type", nullable = false)
    private String configType; // BASE_FARE, SURGE, DISCOUNT, TIME_MULTIPLIER

    @Column(name = "config_key", nullable = false)
    private String configKey;

    @Column(name = "config_value", nullable = false)
    private String configValue;

    @Column(name = "numeric_value", precision = 10, scale = 4)
    private BigDecimal numericValue;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Priority for applying this config (lower = higher priority).
     */
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 100;
}
