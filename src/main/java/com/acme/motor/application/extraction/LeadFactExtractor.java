package com.acme.motor.application.extraction;

import com.acme.motor.config.TenantConfig;
import com.acme.motor.domain.model.Message;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LeadFactExtractor {

    private static final Pattern NAME_FROM_CITY = Pattern.compile(
            "(?iu)\\bsou\\s+([\\p{L}]+(?:\\s+[\\p{L}]+){1,3})\\s+de\\s+([\\p{L}]+(?:\\s+[\\p{L}]+){0,3})");
    private static final Pattern NAME_COMMA_CITY = Pattern.compile(
            "(?u)^\\s*([\\p{Lu}][\\p{L}'-]+(?:\\s+[\\p{Lu}][\\p{L}'-]+){1,3}),"
                    + "\\s*(?:de\\s+)?([\\p{Lu}][\\p{L}'-]+(?:\\s+[\\p{Lu}][\\p{L}'-]+){0,3})"
                    + "\\s*[.!]?\\s*$");
    private static final Pattern EXPLICIT_NAME = Pattern.compile(
            "(?iu)\\b(?:me chamo|meu nome (?:e|é)|sou o|sou a)\\s+([\\p{L}]+(?:\\s+[\\p{L}]+){1,3})");
    private static final Pattern BILL = Pattern.compile(
            "(?iu)(?:conta[^\\d]{0,30}|r\\$\\s*|uns?\\s+)(\\d{2,5})(?:[,.](\\d{1,2}))?");
    private static final Pattern OWNED_PROPERTY = Pattern.compile(
            "\\b(?:casa propria|imovel proprio|imovel (?:e|eh) proprio|"
                    + "(?:casa|imovel) (?:e|eh) (?:minha|meu)|"
                    + "(?:e|eh) minha,? quitada|(?:e|eh) meu,? quitado)\\b");

    public ExtractedFacts extract(List<Message> messages, TenantConfig tenant) {
        Map<String, Object> fields = new LinkedHashMap<>();
        String joined = messages.stream().map(Message::getText).reduce("", (a, b) -> a + "\n" + b);
        String normalized = normalize(joined);

        findName(messages).ifPresent(value -> fields.put("nome", value));
        findCity(messages, tenant).ifPresent(value -> fields.put("cidade", value));

        if (containsAny(normalized, "alugado", "alugada", "aluguel", "moro de aluguel")) {
            fields.put("situacao_imovel", "ALUGADO");
        } else if (OWNED_PROPERTY.matcher(normalized).find()) {
            fields.put("situacao_imovel", "PROPRIO");
        }

        if (containsAny(normalized, "apartamento", "apto")) {
            fields.put("tipo_imovel", "APARTAMENTO");
        } else if (Pattern.compile("\\bcasa\\b").matcher(normalized).find()) {
            fields.put("tipo_imovel", "CASA");
        }

        findBill(joined).ifPresent(value -> fields.put("valor_conta", value));

        if (normalized.contains("financ")) {
            fields.put("preferencia_pagamento", "FINANCIAMENTO");
        } else if (containsAny(normalized, "a vista", "avista")) {
            fields.put("preferencia_pagamento", "A_VISTA");
        }

        boolean existingCustomer = containsAny(normalized, "ja instalei", "ja sou cliente",
                "cliente antigo", "cliente antiga", "minha placa", "minhas placas");
        boolean ambiguous = containsAny(normalized, "inventario", "nome de outra pessoa",
                "nao sei o valor", "nao sei quanto", "nao sei minha conta");

        return new ExtractedFacts(fields, existingCustomer, ambiguous, findRequestedSlot(normalized));
    }

    private Optional<String> findName(List<Message> messages) {
        for (Message message : messages) {
            String text = message.getText().trim();
            for (Pattern pattern : List.of(NAME_FROM_CITY, NAME_COMMA_CITY, EXPLICIT_NAME)) {
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String candidate = titleCase(matcher.group(1).trim());
                    if (!normalize(candidate).startsWith("ignore suas")) {
                        return Optional.of(candidate);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> findCity(List<Message> messages, TenantConfig tenant) {
        String joined = messages.stream().map(Message::getText).reduce("", (a, b) -> a + " " + b);
        String normalized = normalize(joined);
        for (String city : tenant.supportedCities()) {
            if (normalized.contains(normalize(city))) {
                return Optional.of(city);
            }
        }
        for (Message message : messages) {
            String text = message.getText().trim();
            for (Pattern pattern : List.of(NAME_FROM_CITY, NAME_COMMA_CITY)) {
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String city = matcher.group(2).trim().replaceAll("[,.!?].*$", "");
                    city = city.replaceAll("(?iu)\\s+(casa|apartamento|imovel).*$", "").trim();
                    if (!city.isBlank()) {
                        return Optional.of(titleCase(city));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<BigDecimal> findBill(String text) {
        Matcher matcher = BILL.matcher(text);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String decimal = matcher.group(2) == null ? "00" : matcher.group(2);
        return Optional.of(new BigDecimal(matcher.group(1) + "." + decimal));
    }

    private String findRequestedSlot(String normalized) {
        if (normalized.contains("quinta") && containsAny(normalized, "manha", "de manha")) {
            return "quinta de manhã";
        }
        if (normalized.contains("quinta") && normalized.contains("14")) {
            return "quinta às 14h";
        }
        if (normalized.contains("sexta") && normalized.contains("10")) {
            return "sexta às 10h";
        }
        return null;
    }

    public static String normalize(String value) {
        String decomposed = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private String titleCase(String value) {
        StringBuilder result = new StringBuilder();
        for (String word : value.toLowerCase(Locale.forLanguageTag("pt-BR")).split("\\s+")) {
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
