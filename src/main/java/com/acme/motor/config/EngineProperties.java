package com.acme.motor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "engine")
public record EngineProperties(
        Duration burstWindow,
        Duration schedulerDelay,
        boolean schedulerEnabled,
        Llm llm,
        List<String> availableSlots
) {
    public EngineProperties {
        burstWindow = burstWindow == null ? Duration.ofSeconds(8) : burstWindow;
        schedulerDelay = schedulerDelay == null ? Duration.ofMillis(500) : schedulerDelay;
        llm = llm == null ? new Llm(null, null, null, null, null, null) : llm;
        availableSlots = availableSlots == null || availableSlots.isEmpty()
                ? List.of("quinta de manhã", "quinta às 14h", "sexta às 10h")
                : List.copyOf(availableSlots);
    }

    public record Llm(
            String provider,
            String baseUrl,
            String apiKey,
            String model,
            BigDecimal inputCostPerMillion,
            BigDecimal outputCostPerMillion
    ) {
        public Llm {
            provider = provider == null || provider.isBlank() ? "template" : provider;
            model = model == null || model.isBlank() ? "template-v1" : model;
            inputCostPerMillion = inputCostPerMillion == null ? BigDecimal.ZERO : inputCostPerMillion;
            outputCostPerMillion = outputCostPerMillion == null ? BigDecimal.ZERO : outputCostPerMillion;
        }
    }
}
