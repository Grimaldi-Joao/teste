package com.acme.motor.infrastructure.llm;

import com.acme.motor.config.EngineProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "engine.llm.provider", havingValue = "openai-compatible")
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final EngineProperties properties;
    private final RestClient client;

    public OpenAiCompatibleLlmClient(EngineProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        if (properties.llm().baseUrl() == null || properties.llm().baseUrl().isBlank()) {
            throw new IllegalStateException("LLM_BASE_URL é obrigatório para o provider openai-compatible");
        }
        RestClient.Builder configured = builder.baseUrl(properties.llm().baseUrl());
        if (properties.llm().apiKey() != null && !properties.llm().apiKey().isBlank()) {
            configured.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.llm().apiKey());
        }
        this.client = configured.build();
    }

    @Override
    public LlmResult compose(LlmPrompt prompt) {
        long started = System.nanoTime();
        Map<String, Object> request = Map.of(
                "model", properties.llm().model(),
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", prompt.systemInstructions()),
                        Map.of("role", "user", "content", prompt.conversationContext()
                                + "\n\nResposta segura de referência:\n" + prompt.safeDraft()
                                + "\n\nReescreva sem acrescentar fatos, preços, descontos ou promessas.")
                )
        );
        JsonNode response = client.post()
                .uri("/chat/completions")
                .body(request)
                .retrieve()
                .body(JsonNode.class);
        if (response == null || response.path("choices").isEmpty()) {
            throw new IllegalStateException("Provedor LLM retornou uma resposta vazia");
        }
        String text = response.path("choices").path(0).path("message").path("content").asText();
        int input = response.path("usage").path("prompt_tokens").asInt(Math.max(1, prompt.conversationContext().length() / 4));
        int output = response.path("usage").path("completion_tokens").asInt(Math.max(1, text.length() / 4));
        BigDecimal cost = cost(input, output);
        return new LlmResult(text, properties.llm().model(), input, output, cost,
                (System.nanoTime() - started) / 1_000_000);
    }

    private BigDecimal cost(int inputTokens, int outputTokens) {
        BigDecimal input = properties.llm().inputCostPerMillion()
                .multiply(BigDecimal.valueOf(inputTokens));
        BigDecimal output = properties.llm().outputCostPerMillion()
                .multiply(BigDecimal.valueOf(outputTokens));
        return input.add(output).divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);
    }
}
