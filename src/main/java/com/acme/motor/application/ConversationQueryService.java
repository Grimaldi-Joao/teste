package com.acme.motor.application;

import com.acme.motor.domain.model.Appointment;
import com.acme.motor.domain.model.Conversation;
import com.acme.motor.domain.model.ExecutedAction;
import com.acme.motor.domain.model.LeadProfile;
import com.acme.motor.domain.model.Message;
import com.acme.motor.domain.model.TurnMetric;
import com.acme.motor.domain.repository.AppointmentRepository;
import com.acme.motor.domain.repository.ConversationRepository;
import com.acme.motor.domain.repository.ExecutedActionRepository;
import com.acme.motor.domain.repository.LeadProfileRepository;
import com.acme.motor.domain.repository.MessageRepository;
import com.acme.motor.domain.repository.TurnMetricRepository;
import com.acme.motor.support.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class ConversationQueryService {

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final LeadProfileRepository leads;
    private final ExecutedActionRepository actions;
    private final TurnMetricRepository metrics;
    private final AppointmentRepository appointments;

    public ConversationQueryService(ConversationRepository conversations, MessageRepository messages,
                                    LeadProfileRepository leads, ExecutedActionRepository actions,
                                    TurnMetricRepository metrics, AppointmentRepository appointments) {
        this.conversations = conversations;
        this.messages = messages;
        this.leads = leads;
        this.actions = actions;
        this.metrics = metrics;
        this.appointments = appointments;
    }

    @Transactional(readOnly = true)
    public ConversationView get(String tenantId, String phone) {
        Conversation conversation = conversations.findByTenantIdAndPhone(tenantId, phone)
                .orElseThrow(() -> new NotFoundException("Conversa não encontrada"));
        LeadProfile profile = leads.findByConversationId(conversation.getId()).orElse(null);
        return new ConversationView(
                conversation.getId(), conversation.getTenantId(), conversation.getPhone(),
                conversation.getStatus().name(), conversation.getStage(), List.copyOf(conversation.getTags()),
                conversation.isSimulation(), conversation.getProcessAfter(),
                profile == null ? null : toLead(profile),
                messages.findByConversationIdOrderByOccurredAtAscIdAsc(conversation.getId()).stream()
                        .map(this::toMessage).toList(),
                actions.findByConversationIdOrderByExecutedAtAscIdAsc(conversation.getId()).stream()
                        .map(this::toAction).toList(),
                appointments.findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                        .map(this::toAppointment).toList(),
                metrics.findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                        .map(this::toMetric).toList()
        );
    }

    private LeadView toLead(LeadProfile lead) {
        return new LeadView(lead.getName(), lead.getCity(), lead.getPropertyOwnership(), lead.getPropertyType(),
                lead.getMonthlyBill(), lead.getPaymentPreference(), lead.getUpdatedAt());
    }

    private MessageView toMessage(Message message) {
        return new MessageView(message.getExternalId(), message.getAuthor().name().toLowerCase(),
                message.getText(), message.getOccurredAt(), message.isProcessed());
    }

    private ActionView toAction(ExecutedAction action) {
        return new ActionView(action.getActionType().name().toLowerCase(), action.getPayload(),
                action.isSimulation(), action.getExecutedAt(), action.getTurnCorrelationId());
    }

    private AppointmentView toAppointment(Appointment appointment) {
        return new AppointmentView(appointment.getSlot(), appointment.getCreatedAt());
    }

    private TurnMetricView toMetric(TurnMetric metric) {
        return new TurnMetricView(metric.getModel(), metric.getInputTokens(), metric.getOutputTokens(),
                metric.getEstimatedCost(), metric.getLatencyMs(), metric.getActionsCount(), metric.getCreatedAt());
    }

    public record ConversationView(Long id, String tenantId, String telefone, String status, String estagio,
                                   List<String> tags, boolean simulacao, Instant processamentoAgendadoPara,
                                   LeadView ficha, List<MessageView> mensagens, List<ActionView> acoes,
                                   List<AppointmentView> agendamentos, List<TurnMetricView> turnos) {
    }

    public record LeadView(String nome, String cidade, String situacaoImovel, String tipoImovel,
                           BigDecimal valorConta, String preferenciaPagamento, Instant atualizadoEm) {
    }

    public record MessageView(String messageId, String autor, String texto, Instant timestamp, boolean processada) {
    }

    public record ActionView(String acao, String payload, boolean simulacao, Instant executadaEm,
                             String turnoId) {
    }

    public record AppointmentView(String horario, Instant criadoEm) {
    }

    public record TurnMetricView(String modelo, int tokensEntrada, int tokensSaida, BigDecimal custoEstimado,
                                 long latenciaMs, int acoesExecutadas, Instant criadoEm) {
    }
}
