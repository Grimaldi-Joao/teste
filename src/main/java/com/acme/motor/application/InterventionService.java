package com.acme.motor.application;

import com.acme.motor.config.EngineProperties;
import com.acme.motor.config.TenantConfigService;
import com.acme.motor.domain.model.ActionType;
import com.acme.motor.domain.model.Conversation;
import com.acme.motor.domain.model.ExecutedAction;
import com.acme.motor.domain.model.MessageAuthor;
import com.acme.motor.domain.repository.ConversationRepository;
import com.acme.motor.domain.repository.ExecutedActionRepository;
import com.acme.motor.domain.repository.MessageRepository;
import com.acme.motor.support.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class InterventionService {

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final ExecutedActionRepository actions;
    private final TenantConfigService tenants;
    private final EngineProperties properties;

    public InterventionService(ConversationRepository conversations, MessageRepository messages,
                               ExecutedActionRepository actions, TenantConfigService tenants,
                               EngineProperties properties) {
        this.conversations = conversations;
        this.messages = messages;
        this.actions = actions;
        this.tenants = tenants;
        this.properties = properties;
    }

    @Transactional
    public InterventionResult intervene(String tenantId, String phone, String requestedAction) {
        tenants.getRequired(tenantId);
        Conversation conversation = conversations.findByTenantIdAndPhone(tenantId, phone)
                .orElseThrow(() -> new NotFoundException("Conversa não encontrada"));
        String action = requestedAction.toLowerCase(Locale.ROOT);
        String correlation = UUID.randomUUID().toString();
        if ("assumir".equals(action)) {
            conversation.humanAssumed();
            audit(conversation, correlation, ActionType.INTERVENCAO_ASSUMIR, "Intervenção humana solicitada");
        } else if ("religar".equals(action)) {
            boolean hasPending = !messages
                    .findByConversationIdAndProcessedFalseAndAuthorInOrderByOccurredAtAscIdAsc(
                            conversation.getId(), List.of(MessageAuthor.LEAD))
                    .isEmpty();
            conversation.reconnectAi(hasPending ? Instant.now().plus(properties.burstWindow()) : null);
            audit(conversation, correlation, ActionType.INTERVENCAO_RELIGAR, "IA religada");
        } else {
            throw new IllegalArgumentException("A ação deve ser 'assumir' ou 'religar'");
        }
        conversations.save(conversation);
        return new InterventionResult(conversation.getId(), conversation.getStatus().name(), conversation.getStage());
    }

    private void audit(Conversation conversation, String correlation, ActionType type, String detail) {
        actions.save(new ExecutedAction(conversation, correlation, type,
                "{\"detalhe\":\"" + detail + "\"}", conversation.isSimulation()));
    }

    public record InterventionResult(Long conversationId, String status, String stage) {
    }
}
