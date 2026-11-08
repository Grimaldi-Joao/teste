package com.acme.motor.application.qualification;

public record QualificationOutcome(Status status, String reason, String missingField) {
    public enum Status {
        INCOMPLETE,
        QUALIFIED,
        DISQUALIFIED,
        HUMAN_REVIEW
    }
}
