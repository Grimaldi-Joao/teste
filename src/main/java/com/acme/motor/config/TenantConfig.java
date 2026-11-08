package com.acme.motor.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TenantConfig(
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("nome_empresa") String companyName,
        @JsonProperty("tom") String tone,
        @JsonProperty("pode_dizer_que_e_ia") boolean canSayItIsAi,
        @JsonProperty("nome_assistente") String assistantName,
        @JsonProperty("horario_atendimento_ia") String serviceHours,
        @JsonProperty("telefone_notificacao") String notificationPhone,
        @JsonProperty("cidades_atendidas") List<String> supportedCities,
        @JsonProperty("estagios") List<String> stages,
        @JsonProperty("regras_gerais") List<String> generalRules
) {
}
