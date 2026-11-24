package com.smartclassroom.smart_classroom.repository;

import com.smartclassroom.smart_classroom.entity.ConversationMessage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {
    @EntityGraph(attributePaths = {"sender", "receiver"})
    List<ConversationMessage> findByConversation_IdOrderBySentAtAsc(Long conversationId);

    List<ConversationMessage> findByConversation_IdAndReceiver_IdAndReadAtIsNull(
            Long conversationId, Long receiverId);

    long countByConversation_IdAndReceiver_IdAndReadAtIsNull(Long conversationId, Long receiverId);
}
