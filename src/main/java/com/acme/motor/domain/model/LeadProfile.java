package com.acme.motor.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "lead_profiles")
public class LeadProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false, unique = true)
    private Conversation conversation;

    @Column(length = 160)
    private String name;

    @Column(length = 120)
    private String city;

    @Column(name = "property_ownership", length = 32)
    private String propertyOwnership;

    @Column(name = "property_type", length = 32)
    private String propertyType;

    @Column(name = "monthly_bill", precision = 12, scale = 2)
    private BigDecimal monthlyBill;

    @Column(name = "payment_preference", length = 40)
    private String paymentPreference;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LeadProfile() {
    }

    public LeadProfile(Conversation conversation) {
        this.conversation = conversation;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Conversation getConversation() { return conversation; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public String getPropertyOwnership() { return propertyOwnership; }
    public String getPropertyType() { return propertyType; }
    public BigDecimal getMonthlyBill() { return monthlyBill; }
    public String getPaymentPreference() { return paymentPreference; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean update(String field, Object value) {
        boolean changed = switch (field) {
            case "nome" -> setIfDifferent(this.name, (String) value, v -> this.name = v);
            case "cidade" -> setIfDifferent(this.city, (String) value, v -> this.city = v);
            case "tipo_imovel" -> setIfDifferent(this.propertyType, (String) value, v -> this.propertyType = v);
            case "situacao_imovel" -> setIfDifferent(this.propertyOwnership, (String) value, v -> this.propertyOwnership = v);
            case "preferencia_pagamento" -> setIfDifferent(this.paymentPreference, (String) value, v -> this.paymentPreference = v);
            case "valor_conta" -> {
                BigDecimal amount = (BigDecimal) value;
                if (amount != null && amount.compareTo(this.monthlyBill == null ? BigDecimal.valueOf(-1) : this.monthlyBill) != 0) {
                    this.monthlyBill = amount;
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
        if (changed) {
            updatedAt = Instant.now();
        }
        return changed;
    }

    private boolean setIfDifferent(String current, String value, java.util.function.Consumer<String> setter) {
        if (value != null && !value.isBlank() && !value.equals(current)) {
            setter.accept(value);
            return true;
        }
        return false;
    }
}
