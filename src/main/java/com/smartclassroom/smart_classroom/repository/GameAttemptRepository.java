
package com.smartclassroom.smart_classroom.repository;

import com.smartclassroom.smart_classroom.entity.GameAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameAttemptRepository extends JpaRepository<GameAttempt, Long> {

    // Get all attempts made by a student for a particular assignment
    @EntityGraph(attributePaths = {"assignment", "assignment.game"})
    List<GameAttempt> findByAssignment_IdAndStudent_IdOrderByAttemptNumberAsc(
            Long assignmentId, Long studentId);

    // Get all attempts for a particular assignment
    List<GameAttempt> findByAssignment_Id(Long assignmentId);

    @Query(value = "SELECT * FROM game_attempts "
            + "WHERE game_assignment_id = :assignmentId FOR UPDATE",
            nativeQuery = true)
    List<GameAttempt> findByAssignmentIdForUpdate(
            @Param("assignmentId") Long assignmentId);

    @Query(value = "SELECT * FROM game_attempts WHERE id = :id FOR UPDATE",
            nativeQuery = true)
    java.util.Optional<GameAttempt> findByIdForUpdateNative(@Param("id") Long id);

    @EntityGraph(attributePaths = {"assignment", "assignment.game", "student"})
    List<GameAttempt> findByAssignment_IdIn(List<Long> assignmentIds);

    @EntityGraph(attributePaths = {"assignment", "assignment.game"})
    List<GameAttempt> findByAssignment_IdInAndStudent_Id(
            List<Long> assignmentIds, Long studentId);

    // Get all attempts made by a particular student
    @EntityGraph(attributePaths = {"assignment", "assignment.game"})
    List<GameAttempt> findByStudent_IdOrderByStartedAtDesc(Long studentId);

    @EntityGraph(attributePaths = {"assignment", "assignment.game"})
    @Query("select attempt from GameAttempt attempt where attempt.id = :id")
    java.util.Optional<GameAttempt> findWithAssignmentAndGameById(
            @Param("id") Long id);

    // Count attempts made by a student for an assignment
    long countByAssignment_IdAndStudent_Id(Long assignmentId, Long studentId);

    // Get all attempts associated with an assignment
    void deleteByAssignment_Id(Long assignmentId);
}
