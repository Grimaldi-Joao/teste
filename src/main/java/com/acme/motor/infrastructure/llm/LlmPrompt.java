package com.acme.motor.infrastructure.llm;

public record LlmPrompt(String systemInstructions, String conversationContext, String safeDraft) {
}
