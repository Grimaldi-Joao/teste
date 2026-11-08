package com.acme.motor.application;

import java.time.Instant;

public record IngestionResult(boolean duplicate, Long conversationId, Instant scheduledFor, String status) {
}
