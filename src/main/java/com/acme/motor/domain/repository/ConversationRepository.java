package com.acme.motor.domain.repository;

import com.acme.motor.domain.model.Conversation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByTenantIdAndPhone(String tenantId, String phone);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conversation c where c.id = :id")
    Optional<Conversation> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "tags")
    List<Conversation> findAllByTenantId(String tenantId);

    List<Conversation> findTop50ByProcessAfterLessThanEqualOrderByProcessAfterAsc(Instant now);
}
