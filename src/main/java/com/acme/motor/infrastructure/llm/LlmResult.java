package com.acme.motor.infrastructure.llm;

import java.math.BigDecimal;

public record LlmResult(
        String text,
        String model,
        int inputTokens,
        int outputTokens,
        BigDecimal estimatedCost,
        long latencyMs
) {
}
