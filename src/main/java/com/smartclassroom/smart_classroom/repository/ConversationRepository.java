package com.smartclassroom.smart_classroom.repository;

import com.smartclassroom.smart_classroom.entity.Conversation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByTeacher_IdAndParticipant_IdAndStudent_Id(
            Long teacherId, Long participantId, Long studentId);

    @EntityGraph(attributePaths = {"participant", "student"})
    List<Conversation> findByTeacher_IdOrderByUpdatedAtDesc(Long teacherId);

    @EntityGraph(attributePaths = {"teacher", "student"})
    List<Conversation> findByParticipant_IdOrderByUpdatedAtDesc(Long participantId);

    @EntityGraph(attributePaths = {"teacher", "participant", "student"})
    Optional<Conversation> findWithParticipantsById(Long id);
}
