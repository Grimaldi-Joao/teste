package com.acme.motor.domain.repository;

import com.acme.motor.domain.model.Message;
import com.acme.motor.domain.model.MessageAuthor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    boolean existsByExternalId(String externalId);
    List<Message> findByConversationIdOrderByOccurredAtAscIdAsc(Long conversationId);
    List<Message> findByConversationIdAndProcessedFalseAndAuthorInOrderByOccurredAtAscIdAsc(
            Long conversationId, Collection<MessageAuthor> authors);
    long countByConversationIdAndAuthor(Long conversationId, MessageAuthor author);
}
