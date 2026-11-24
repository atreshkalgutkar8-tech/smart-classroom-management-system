package com.smartclassroom.smart_classroom.repository;

import com.smartclassroom.smart_classroom.entity.StudentConversation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentConversationRepository extends JpaRepository<StudentConversation, Long> {

    @EntityGraph(attributePaths = {"teacher", "student"})
    Optional<StudentConversation> findByTeacher_IdAndStudent_Id(Long teacherId, Long studentId);

    @EntityGraph(attributePaths = {"teacher", "student"})
    List<StudentConversation> findByTeacher_IdOrderByUpdatedAtDesc(Long teacherId);

    @EntityGraph(attributePaths = {"teacher", "student"})
    List<StudentConversation> findByStudent_IdOrderByUpdatedAtDesc(Long studentId);

    @EntityGraph(attributePaths = {"teacher", "student"})
    Optional<StudentConversation> findByIdAndTeacher_Id(Long id, Long teacherId);

    @EntityGraph(attributePaths = {"teacher", "student"})
    Optional<StudentConversation> findByIdAndStudent_Id(Long id, Long studentId);
}
