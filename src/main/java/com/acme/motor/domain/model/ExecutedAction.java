package com.acme.motor.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "executed_actions")
public class ExecutedAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "turn_correlation_id", nullable = false, length = 40)
    private String turnCorrelationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private ActionType actionType;

    @Column(nullable = false, length = 4000)
    private String payload;

    @Column(nullable = false)
    private boolean simulation;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    protected ExecutedAction() {
    }

    public ExecutedAction(Conversation conversation, String turnCorrelationId, ActionType actionType,
                          String payload, boolean simulation) {
        this.conversation = conversation;
        this.turnCorrelationId = turnCorrelationId;
        this.actionType = actionType;
        this.payload = payload;
        this.simulation = simulation;
        this.executedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Conversation getConversation() { return conversation; }
    public String getTurnCorrelationId() { return turnCorrelationId; }
    public ActionType getActionType() { return actionType; }
    public String getPayload() { return payload; }
    public boolean isSimulation() { return simulation; }
    public Instant getExecutedAt() { return executedAt; }
}
