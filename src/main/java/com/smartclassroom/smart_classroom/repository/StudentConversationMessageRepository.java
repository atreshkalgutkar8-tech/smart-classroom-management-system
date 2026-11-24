package com.smartclassroom.smart_classroom.repository;

import com.smartclassroom.smart_classroom.entity.StudentConversationMessage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentConversationMessageRepository
        extends JpaRepository<StudentConversationMessage, Long> {

    @EntityGraph(attributePaths = {"sender", "receiver"})
    List<StudentConversationMessage> findByConversation_IdAndConversation_Teacher_IdOrderBySentAtAsc(
            Long conversationId, Long teacherId);

    @EntityGraph(attributePaths = {"sender", "receiver"})
    List<StudentConversationMessage> findByConversation_IdAndConversation_Student_IdOrderBySentAtAsc(
            Long conversationId, Long studentId);

    Optional<StudentConversationMessage> findFirstByConversation_IdAndConversation_Teacher_IdOrderBySentAtDesc(
            Long conversationId, Long teacherId);

    Optional<StudentConversationMessage> findFirstByConversation_IdAndConversation_Student_IdOrderBySentAtDesc(
            Long conversationId, Long studentId);

    List<StudentConversationMessage> findByConversation_IdAndConversation_Teacher_IdAndReceiver_IdAndReadAtIsNull(
            Long conversationId, Long teacherId, Long receiverId);

    List<StudentConversationMessage> findByConversation_IdAndConversation_Student_IdAndReceiver_IdAndReadAtIsNull(
            Long conversationId, Long studentId, Long receiverId);

    long countByConversation_IdAndConversation_Teacher_IdAndReceiver_IdAndReadAtIsNull(
            Long conversationId, Long teacherId, Long receiverId);

    long countByConversation_IdAndConversation_Student_IdAndReceiver_IdAndReadAtIsNull(
            Long conversationId, Long studentId, Long receiverId);

    long countByConversation_Student_IdAndReceiver_IdAndReadAtIsNull(
            Long studentId, Long receiverId);
}
