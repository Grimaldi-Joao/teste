package com.acme.motor.api;

import com.acme.motor.application.ConversationQueryService;
import com.acme.motor.application.MetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConversationController {
    private final ConversationQueryService conversations;
    private final MetricsService metrics;

    public ConversationController(ConversationQueryService conversations, MetricsService metrics) {
        this.conversations = conversations;
        this.metrics = metrics;
    }

    @GetMapping("/conversas/{tenantId}/{telefone}")
    public ConversationQueryService.ConversationView conversation(@PathVariable String tenantId,
                                                                   @PathVariable String telefone) {
        return conversations.get(tenantId, telefone);
    }

    @GetMapping("/metricas/{tenantId}")
    public MetricsService.TenantMetrics metrics(@PathVariable String tenantId) {
        return metrics.get(tenantId);
    }
}
