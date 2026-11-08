package com.acme.motor.api;

import com.acme.motor.application.IncomingMessageCommand;
import com.acme.motor.application.IngestionResult;
import com.acme.motor.application.MessageIngestionService;
import com.acme.motor.domain.model.MessageAuthor;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Locale;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final MessageIngestionService ingestion;

    public WebhookController(MessageIngestionService ingestion) {
        this.ingestion = ingestion;
    }

    @PostMapping("/mensagem")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IngestionResult receive(@Valid @RequestBody WebhookRequest request) {
        return ingestion.receive(new IncomingMessageCommand(
                request.messageId(), request.tenantId(), request.phone(), request.text(),
                request.timestamp() == null ? null : request.timestamp().toInstant(),
                parseAuthor(request.author()), request.simulation()));
    }

    private MessageAuthor parseAuthor(String author) {
        if (author == null || author.isBlank()) return MessageAuthor.LEAD;
        try {
            return MessageAuthor.valueOf(author.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("autor deve ser lead ou humano");
        }
    }

    public record WebhookRequest(
            @NotBlank @JsonProperty("message_id") String messageId,
            @NotBlank @JsonProperty("tenant_id") String tenantId,
            @NotBlank @JsonProperty("telefone") String phone,
            @NotBlank @JsonProperty("texto") String text,
            @JsonProperty("timestamp") OffsetDateTime timestamp,
            @JsonProperty("autor") String author,
            @JsonProperty("simulacao") boolean simulation
    ) {
    }
}
