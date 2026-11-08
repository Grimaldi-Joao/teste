package com.acme.motor.application.qualification;

import com.acme.motor.application.extraction.LeadFactExtractor;
import com.acme.motor.config.TenantConfig;
import com.acme.motor.domain.model.LeadProfile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class QualificationEngine {

    public QualificationOutcome evaluate(LeadProfile lead, TenantConfig tenant, boolean ambiguous) {
        if (ambiguous) {
            return new QualificationOutcome(QualificationOutcome.Status.HUMAN_REVIEW,
                    "Situação do imóvel ou valor da conta exige análise humana", null);
        }
        if ("ALUGADO".equals(lead.getPropertyOwnership())) {
            return new QualificationOutcome(QualificationOutcome.Status.DISQUALIFIED,
                    "Imóvel alugado", null);
        }
        if ("APARTAMENTO".equals(lead.getPropertyType())) {
            return new QualificationOutcome(QualificationOutcome.Status.DISQUALIFIED,
                    "Imóvel é apartamento", null);
        }
        if (lead.getMonthlyBill() != null && lead.getMonthlyBill().compareTo(BigDecimal.valueOf(250)) <= 0) {
            return new QualificationOutcome(QualificationOutcome.Status.DISQUALIFIED,
                    "Conta de luz até R$ 250", null);
        }
        if (lead.getCity() != null && !isSupported(lead.getCity(), tenant)) {
            return new QualificationOutcome(QualificationOutcome.Status.DISQUALIFIED,
                    "Cidade fora da área atendida", null);
        }

        String missing = firstMissing(lead);
        if (missing != null) {
            return new QualificationOutcome(QualificationOutcome.Status.INCOMPLETE, null, missing);
        }

        boolean qualified = "PROPRIO".equals(lead.getPropertyOwnership())
                && "CASA".equals(lead.getPropertyType())
                && lead.getMonthlyBill().compareTo(BigDecimal.valueOf(250)) > 0
                && isSupported(lead.getCity(), tenant);
        return qualified
                ? new QualificationOutcome(QualificationOutcome.Status.QUALIFIED, "Critérios atendidos", null)
                : new QualificationOutcome(QualificationOutcome.Status.HUMAN_REVIEW,
                "Combinação de respostas não prevista", null);
    }

    private String firstMissing(LeadProfile lead) {
        if (lead.getName() == null) return "nome";
        if (lead.getCity() == null) return "cidade";
        if (lead.getPropertyOwnership() == null) return "situacao_imovel";
        if (lead.getPropertyType() == null) return "tipo_imovel";
        if (lead.getMonthlyBill() == null) return "valor_conta";
        if (lead.getPaymentPreference() == null) return "preferencia_pagamento";
        return null;
    }

    private boolean isSupported(String city, TenantConfig tenant) {
        String normalized = LeadFactExtractor.normalize(city);
        return tenant.supportedCities().stream()
                .map(LeadFactExtractor::normalize)
                .anyMatch(normalized::equals);
    }
}
