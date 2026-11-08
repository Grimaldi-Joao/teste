package com.acme.motor.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "messages",
        uniqueConstraints = @UniqueConstraint(name = "uk_message_external_id", columnNames = "external_id"),
        indexes = @Index(name = "idx_message_pending", columnList = "conversation_id,processed"))
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true, length = 160)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageAuthor author;

    @Column(nullable = false, length = 5000)
    private String text;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(nullable = false)
    private boolean processed;

    protected Message() {
    }

    public Message(String externalId, Conversation conversation, MessageAuthor author, String text,
                   Instant occurredAt, Instant receivedAt, boolean processed) {
        this.externalId = externalId;
        this.conversation = conversation;
        this.author = author;
        this.text = text;
        this.occurredAt = occurredAt;
        this.receivedAt = receivedAt;
        this.processed = processed;
    }

    public static Message aiReply(Conversation conversation, String text, Instant now) {
        return new Message(null, conversation, MessageAuthor.IA, text, now, now, true);
    }

    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public Conversation getConversation() { return conversation; }
    public MessageAuthor getAuthor() { return author; }
    public String getText() { return text; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getReceivedAt() { return receivedAt; }
    public boolean isProcessed() { return processed; }

    public void markProcessed() {
        this.processed = true;
    }
}
