
package com.smartclassroom.smart_classroom.repository;

import com.smartclassroom.smart_classroom.entity.GameAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameAssignmentRepository extends JpaRepository<GameAssignment, Long> {

    // Get all assignments created by a particular teacher
    @EntityGraph(attributePaths = "game")
    List<GameAssignment> findByTeacher_EmailOrderByAssignedAtDesc(String email);

    @EntityGraph(attributePaths = "game")
    List<GameAssignment> findByTeacher_EmailAndActiveTrueOrderByAssignedAtDesc(String email);

    // Get assignments for a particular grade and section
    @EntityGraph(attributePaths = "game")
    List<GameAssignment> findByGradeAndSectionIgnoreCaseOrderByDeadlineAsc(
            Integer grade, String section);

    @EntityGraph(attributePaths = "game")
    List<GameAssignment> findByGradeAndSectionIgnoreCaseAndActiveTrueOrderByDeadlineAsc(
            Integer grade, String section);

    List<GameAssignment> findByGradeAndSectionIgnoreCaseAndActiveTrue(
            Integer grade, String section);

    // Find an assignment owned by a particular teacher
    Optional<GameAssignment> findByIdAndTeacher_Email(Long id, String email);

    @Query(value = "SELECT * FROM game_assignments WHERE id = :id FOR UPDATE",
            nativeQuery = true)
    Optional<GameAssignment> findByIdForUpdateNative(@Param("id") Long id);

    // Get assignments associated with a particular game
    List<GameAssignment> findByGame_Id(Long gameId);

    // Count assignments for a particular game
    long countByGame_Id(Long gameId);

    void deleteByGame_Id(Long gameId);
}
