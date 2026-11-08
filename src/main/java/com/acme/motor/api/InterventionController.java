package com.acme.motor.api;

import com.acme.motor.application.InterventionService;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InterventionController {
    private final InterventionService interventions;

    public InterventionController(InterventionService interventions) {
        this.interventions = interventions;
    }

    @PostMapping("/intervencao")
    public InterventionService.InterventionResult intervene(@Valid @RequestBody InterventionRequest request) {
        return interventions.intervene(request.tenantId(), request.phone(), request.action());
    }

    public record InterventionRequest(
            @NotBlank @JsonProperty("tenant_id") String tenantId,
            @NotBlank @JsonProperty("telefone") String phone,
            @NotBlank @JsonProperty("acao") String action
    ) {
    }
}
