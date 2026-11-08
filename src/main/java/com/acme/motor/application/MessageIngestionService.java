package com.acme.motor.application;

import com.acme.motor.config.EngineProperties;
import com.acme.motor.config.TenantConfigService;
import com.acme.motor.domain.model.Conversation;
import com.acme.motor.domain.model.ConversationStatus;
import com.acme.motor.domain.model.Message;
import com.acme.motor.domain.model.MessageAuthor;
import com.acme.motor.domain.repository.ConversationRepository;
import com.acme.motor.domain.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class MessageIngestionService {

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final TenantConfigService tenants;
    private final EngineProperties properties;
    private final Clock clock;

    public MessageIngestionService(ConversationRepository conversations, MessageRepository messages,
                                   TenantConfigService tenants, EngineProperties properties) {
        this.conversations = conversations;
        this.messages = messages;
        this.tenants = tenants;
        this.properties = properties;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public IngestionResult receive(IncomingMessageCommand command) {
        tenants.getRequired(command.tenantId());
        if (messages.existsByExternalId(command.messageId())) {
            return new IngestionResult(true, null, null, "duplicada");
        }

        Instant now = clock.instant();
        Conversation conversation = conversations.findByTenantIdAndPhone(command.tenantId(), command.phone())
                .orElseGet(() -> conversations.save(new Conversation(command.tenantId(), command.phone(), now)));
        MessageAuthor author = command.author() == null ? MessageAuthor.LEAD : command.author();
        boolean processed = author != MessageAuthor.LEAD;
        messages.save(new Message(command.messageId(), conversation, author, command.text(),
                command.timestamp() == null ? now : command.timestamp(), now, processed));

        Instant scheduled = null;
        if (author == MessageAuthor.LEAD && conversation.getStatus() == ConversationStatus.AI_ACTIVE) {
            scheduled = now.plus(properties.burstWindow());
            conversation.scheduleFor(scheduled, command.simulation());
            conversations.save(conversation);
        }

        String status = conversation.getStatus() == ConversationStatus.HUMAN_ACTIVE
                ? "armazenada_sem_resposta_ia"
                : (author == MessageAuthor.HUMANO ? "mensagem_humana_armazenada" : "agendada");
        return new IngestionResult(false, conversation.getId(), scheduled, status);
    }
}
