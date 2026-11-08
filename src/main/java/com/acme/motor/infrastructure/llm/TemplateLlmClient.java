package com.acme.motor.infrastructure.llm;

import com.acme.motor.config.EngineProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "engine.llm.provider", havingValue = "template", matchIfMissing = true)
public class TemplateLlmClient implements LlmClient {

    private final EngineProperties properties;

    public TemplateLlmClient(EngineProperties properties) {
        this.properties = properties;
    }

    @Override
    public LlmResult compose(LlmPrompt prompt) {
        long started = System.nanoTime();
        int input = estimate(prompt.systemInstructions() + prompt.conversationContext() + prompt.safeDraft());
        int output = estimate(prompt.safeDraft());
        return new LlmResult(prompt.safeDraft(), properties.llm().model(), input, output,
                BigDecimal.ZERO, (System.nanoTime() - started) / 1_000_000);
    }

    private int estimate(String text) {
        return Math.max(1, text.length() / 4);
    }
}
