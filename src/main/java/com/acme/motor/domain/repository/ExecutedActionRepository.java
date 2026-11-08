package com.acme.motor.domain.repository;

import com.acme.motor.domain.model.ActionType;
import com.acme.motor.domain.model.ExecutedAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExecutedActionRepository extends JpaRepository<ExecutedAction, Long> {
    List<ExecutedAction> findByConversationIdOrderByExecutedAtAscIdAsc(Long conversationId);
    boolean existsByConversationIdAndActionType(Long conversationId, ActionType actionType);
    long countByConversationIdAndActionType(Long conversationId, ActionType actionType);
}
