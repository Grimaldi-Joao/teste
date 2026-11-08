package com.acme.motor.application;

import com.acme.motor.domain.model.MessageAuthor;

import java.time.Instant;

public record IncomingMessageCommand(
        String messageId,
        String tenantId,
        String phone,
        String text,
        Instant timestamp,
        MessageAuthor author,
        boolean simulation
) {
}
