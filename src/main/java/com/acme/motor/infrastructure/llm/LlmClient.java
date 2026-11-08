package com.acme.motor.infrastructure.llm;

public interface LlmClient {
    LlmResult compose(LlmPrompt prompt);
}
