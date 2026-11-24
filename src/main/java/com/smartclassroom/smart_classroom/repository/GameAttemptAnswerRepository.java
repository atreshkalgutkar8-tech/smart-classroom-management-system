
package com.smartclassroom.smart_classroom.repository;

import com.smartclassroom.smart_classroom.entity.GameAttemptAnswer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameAttemptAnswerRepository
        extends JpaRepository<GameAttemptAnswer, Long> {

    // Get all saved answers for an attempt
    List<GameAttemptAnswer> findByAttempt_IdOrderByQuestion_QuestionOrderAsc(
            Long attemptId);

    // Find a student's saved answer for a particular question
    Optional<GameAttemptAnswer> findByAttempt_IdAndQuestion_Id(
            Long attemptId, Long questionId);

    // Count answered questions in an attempt
    long countByAttempt_Id(Long attemptId);

    long countByAttempt_IdAndEvaluatedTrue(Long attemptId);

    // Delete all saved answers belonging to an attempt
    void deleteByAttempt_Id(Long attemptId);

    long countByQuestion_Id(Long questionId);
}
