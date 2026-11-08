package com.acme.motor.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "turn_metrics")
public class TurnMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "correlation_id", nullable = false, unique = true, length = 40)
    private String correlationId;

    @Column(nullable = false, length = 120)
    private String model;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "estimated_cost", nullable = false, precision = 16, scale = 8)
    private BigDecimal estimatedCost;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "actions_count", nullable = false)
    private int actionsCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TurnMetric() {
    }

    public TurnMetric(Conversation conversation, String correlationId, String model, int inputTokens,
                      int outputTokens, BigDecimal estimatedCost, long latencyMs, int actionsCount) {
        this.conversation = conversation;
        this.correlationId = correlationId;
        this.model = model;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.estimatedCost = estimatedCost;
        this.latencyMs = latencyMs;
        this.actionsCount = actionsCount;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Conversation getConversation() { return conversation; }
    public String getCorrelationId() { return correlationId; }
    public String getModel() { return model; }
    public int getInputTokens() { return inputTokens; }
    public int getOutputTokens() { return outputTokens; }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public long getLatencyMs() { return latencyMs; }
    public int getActionsCount() { return actionsCount; }
    public Instant getCreatedAt() { return createdAt; }
}
