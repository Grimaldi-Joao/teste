package com.acme.motor.domain.repository;

import com.acme.motor.domain.model.LeadProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeadProfileRepository extends JpaRepository<LeadProfile, Long> {
    Optional<LeadProfile> findByConversationId(Long conversationId);
}
