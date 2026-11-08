package com.acme.motor.application.extraction;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ExtractedFacts(
        Map<String, Object> fields,
        boolean existingCustomer,
        boolean ambiguousSituation,
        String requestedSlot
) {
    public ExtractedFacts {
        fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }
}
