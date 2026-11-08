package com.acme.motor.config;

import com.acme.motor.support.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TenantConfigService {

    private final Map<String, TenantConfig> tenants;

    public TenantConfigService(ObjectMapper objectMapper) {
        this.tenants = Collections.unmodifiableMap(loadTenants(objectMapper));
    }

    public TenantConfig getRequired(String tenantId) {
        TenantConfig config = tenants.get(tenantId);
        if (config == null) {
            throw new NotFoundException("Tenant não configurado: " + tenantId);
        }
        return config;
    }

    public Map<String, TenantConfig> getAll() {
        return tenants;
    }

    private Map<String, TenantConfig> loadTenants(ObjectMapper objectMapper) {
        Map<String, TenantConfig> loaded = new LinkedHashMap<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:tenants/*.json");
            for (Resource resource : resources) {
                TenantConfig config = objectMapper.readValue(resource.getInputStream(), TenantConfig.class);
                loaded.put(config.tenantId(), config);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível carregar as configurações dos tenants", exception);
        }
        if (loaded.isEmpty()) {
            throw new IllegalStateException("Nenhum tenant foi configurado");
        }
        return loaded;
    }
}
