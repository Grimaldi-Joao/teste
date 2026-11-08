package com.acme.motor.application;

import com.acme.motor.config.EngineProperties;
import com.acme.motor.domain.model.Conversation;
import com.acme.motor.domain.repository.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class BurstScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(BurstScheduler.class);

    private final ConversationRepository conversations;
    private final ConversationProcessor processor;
    private final EngineProperties properties;

    public BurstScheduler(ConversationRepository conversations, ConversationProcessor processor,
                          EngineProperties properties) {
        this.conversations = conversations;
        this.processor = processor;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${engine.scheduler-delay:500ms}")
    public void processDueConversations() {
        if (!properties.schedulerEnabled()) {
            return;
        }
        for (Conversation conversation : conversations
                .findTop50ByProcessAfterLessThanEqualOrderByProcessAfterAsc(Instant.now())) {
            try {
                processor.processById(conversation.getId());
            } catch (RuntimeException exception) {
                LOGGER.error("Falha ao processar conversa {}", conversation.getId(), exception);
            }
        }
    }
}
