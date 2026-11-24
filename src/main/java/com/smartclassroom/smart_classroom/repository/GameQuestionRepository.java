
package com.smartclassroom.smart_classroom.repository;

import com.smartclassroom.smart_classroom.entity.GameQuestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameQuestionRepository
        extends JpaRepository<GameQuestion, Long> {

    // Get all questions belonging to a particular game
    List<GameQuestion> findByGameIdOrderByQuestionOrderAsc(
            Long gameId
    );


    // Count the number of questions in a game
    long countByGameId(Long gameId);

    Optional<GameQuestion> findFirstByGameIdOrderByQuestionOrderDesc(Long gameId);

    long countByGame_Id(Long gameId);

    // Delete all questions belonging to a particular game
    void deleteByGameId(Long gameId);
}
