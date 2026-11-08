package com.acme.motor.infrastructure.llm;

import com.acme.motor.application.extraction.LeadFactExtractor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ResponseGuard {
    private static final List<String> FORBIDDEN = List.of(
            "50% de desconto", "garanto que", "garantimos que", "sua conta vai zerar",
            "preco final e", "preço final é"
    );

    public String enforce(String candidate, String safeDraft) {
        if (candidate == null || candidate.isBlank()) {
            return safeDraft;
        }
        String normalized = LeadFactExtractor.normalize(candidate);
        if (FORBIDDEN.stream().map(LeadFactExtractor::normalize).anyMatch(normalized::contains)) {
            return safeDraft;
        }
        return candidate.trim();
    }
}
