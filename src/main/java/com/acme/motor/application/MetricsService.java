package com.acme.motor.application;

import com.acme.motor.domain.model.ActionType;
import com.acme.motor.domain.model.Conversation;
import com.acme.motor.domain.model.TurnMetric;
import com.acme.motor.domain.repository.ConversationRepository;
import com.acme.motor.domain.repository.ExecutedActionRepository;
import com.acme.motor.domain.repository.TurnMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class MetricsService {
    private final ConversationRepository conversations;
    private final TurnMetricRepository turns;
    private final ExecutedActionRepository actions;

    public MetricsService(ConversationRepository conversations, TurnMetricRepository turns,
                          ExecutedActionRepository actions) {
        this.conversations = conversations;
        this.turns = turns;
        this.actions = actions;
    }

    @Transactional(readOnly = true)
    public TenantMetrics get(String tenantId) {
        List<Conversation> tenantConversations = conversations.findAllByTenantId(tenantId);
        long total = tenantConversations.size();
        long qualified = tenantConversations.stream().filter(c -> c.getTags().contains("qualificado")).count();
        long transferred = tenantConversations.stream()
                .filter(c -> actions.existsByConversationIdAndActionType(c.getId(), ActionType.TRANSFERIR_HUMANO))
                .count();
        List<TurnMetric> tenantTurns = turns.findByConversationTenantId(tenantId);
        BigDecimal totalCost = tenantTurns.stream().map(TurnMetric::getEstimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long inputTokens = tenantTurns.stream().mapToLong(TurnMetric::getInputTokens).sum();
        long outputTokens = tenantTurns.stream().mapToLong(TurnMetric::getOutputTokens).sum();
        double averageLatency = tenantTurns.stream().mapToLong(TurnMetric::getLatencyMs).average().orElse(0);
        return new TenantMetrics(tenantId, total, tenantTurns.size(), totalCost,
                total == 0 ? BigDecimal.ZERO : totalCost.divide(BigDecimal.valueOf(total), 8, RoundingMode.HALF_UP),
                percentage(qualified, total), percentage(transferred, total), inputTokens, outputTokens,
                BigDecimal.valueOf(averageLatency).setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal percentage(long value, long total) {
        if (total == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(value * 100).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    public record TenantMetrics(String tenantId, long totalConversas, long totalTurnos, BigDecimal custoTotal,
                                BigDecimal custoMedioPorConversa, BigDecimal percentualQualificados,
                                BigDecimal percentualTransferidosHumano, long tokensEntrada, long tokensSaida,
                                BigDecimal latenciaMediaMs) {
    }
}
