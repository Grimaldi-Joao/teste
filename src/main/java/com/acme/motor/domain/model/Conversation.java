package com.acme.motor.domain.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.JoinColumn;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "conversations",
        uniqueConstraints = @UniqueConstraint(name = "uk_conversation_tenant_phone", columnNames = {"tenant_id", "phone"}),
        indexes = @Index(name = "idx_conversation_due", columnList = "process_after"))
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 80)
    private String tenantId;

    @Column(name = "phone", nullable = false, length = 32)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ConversationStatus status = ConversationStatus.AI_ACTIVE;

    @Column(nullable = false, length = 40)
    private String stage = "Novo";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "conversation_tags", joinColumns = @JoinColumn(name = "conversation_id"))
    @Column(name = "tag", nullable = false, length = 80)
    private Set<String> tags = new LinkedHashSet<>();

    @Column(name = "process_after")
    private Instant processAfter;

    @Column(nullable = false)
    private boolean simulation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Conversation() {
    }

    public Conversation(String tenantId, String phone, Instant now) {
        this.tenantId = tenantId;
        this.phone = phone;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getPhone() { return phone; }
    public ConversationStatus getStatus() { return status; }
    public String getStage() { return stage; }
    public Set<String> getTags() { return tags; }
    public Instant getProcessAfter() { return processAfter; }
    public boolean isSimulation() { return simulation; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void scheduleFor(Instant instant, boolean simulation) {
        this.processAfter = instant;
        this.simulation = simulation;
        touch();
    }

    public void clearSchedule() {
        this.processAfter = null;
        touch();
    }

    public void moveTo(String stage) {
        this.stage = stage;
        touch();
    }

    public void addTag(String tag) {
        this.tags.add(tag);
        touch();
    }

    public void humanAssumed() {
        this.status = ConversationStatus.HUMAN_ACTIVE;
        this.stage = "Humano";
        this.processAfter = null;
        touch();
    }

    public void reconnectAi(Instant processAfter) {
        this.status = ConversationStatus.AI_ACTIVE;
        this.processAfter = processAfter;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
