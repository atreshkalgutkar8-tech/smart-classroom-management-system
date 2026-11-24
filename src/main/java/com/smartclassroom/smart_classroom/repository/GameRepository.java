
package com.smartclassroom.smart_classroom.repository;

import com.smartclassroom.smart_classroom.entity.Game;
import com.smartclassroom.smart_classroom.entity.GameSource;
import com.smartclassroom.smart_classroom.entity.GameVisibility;
import com.smartclassroom.smart_classroom.entity.GameType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findBySourceOrderByGradeAscTitleAsc(GameSource source);

    List<Game> findBySourceAndCreatedByIdOrderByGradeAscTitleAsc(
            GameSource source, Long teacherId);

    boolean existsBySourceAndTitleAndGradeAndSubjectAndGameType(
            GameSource source, String title, Integer grade,
            String subject, GameType gameType);

    Optional<Game> findFirstBySourceAndTitleAndGradeAndSubjectAndGameType(
            GameSource source, String title, Integer grade,
            String subject, GameType gameType);

    // Find all preloaded games for a particular grade
    List<Game> findBySourceAndGrade(
            GameSource source,
            Integer grade
    );

    List<Game> findBySourceAndGradeAndVisibility(
            GameSource source, Integer grade, GameVisibility visibility);

    // Find all custom games created by a particular teacher
    List<Game> findByCreatedById(Long teacherId);

    // Find custom games created by a particular teacher
    // filtered by grade
    List<Game> findByCreatedByIdAndGrade(
            Long teacherId,
            Integer grade
    );

    List<Game> findByCreatedByIdAndGradeAndVisibility(
            Long teacherId, Integer grade, GameVisibility visibility);

    List<Game> findBySourceAndCreatedByIdAndGradeAndVisibility(
            GameSource source, Long teacherId, Integer grade,
            GameVisibility visibility);

    // Find games available to students for a particular grade
    List<Game> findByGradeAndVisibility(
            Integer grade,
            GameVisibility visibility
    );

    // Find games by type and grade
    List<Game> findByGameTypeAndGrade(
            GameType gameType,
            Integer grade
    );

    // Find games created by a teacher with a particular visibility
    List<Game> findByCreatedByIdAndVisibility(
            Long teacherId,
            GameVisibility visibility
    );
}
