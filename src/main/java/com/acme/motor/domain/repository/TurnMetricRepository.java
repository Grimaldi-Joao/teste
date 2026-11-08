package com.acme.motor.domain.repository;

import com.acme.motor.domain.model.TurnMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TurnMetricRepository extends JpaRepository<TurnMetric, Long> {
    List<TurnMetric> findByConversationTenantId(String tenantId);
    List<TurnMetric> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
