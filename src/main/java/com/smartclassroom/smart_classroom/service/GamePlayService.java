
package com.smartclassroom.smart_classroom.service;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartclassroom.smart_classroom.dto.GamePlayQuestionDTO;
import com.smartclassroom.smart_classroom.entity.GameAttempt;
import com.smartclassroom.smart_classroom.entity.GameQuestion;
import com.smartclassroom.smart_classroom.entity.GameAttemptAnswer;
import com.smartclassroom.smart_classroom.entity.GameType;
import com.smartclassroom.smart_classroom.entity.Role;
import com.smartclassroom.smart_classroom.repository.GameAttemptRepository;
import com.smartclassroom.smart_classroom.repository.GameQuestionRepository;
import com.smartclassroom.smart_classroom.repository.GameAttemptAnswerRepository;
import com.smartclassroom.smart_classroom.repository.UserRepository;

@Service
public class GamePlayService {

    private final GameAttemptRepository attemptRepository;
    private final GameQuestionRepository questionRepository;
    private final GameAttemptAnswerRepository answerRepository;
    private final UserRepository userRepository;

    public GamePlayService(
            GameAttemptRepository attemptRepository,
            GameQuestionRepository questionRepository,
            GameAttemptAnswerRepository answerRepository,
            UserRepository userRepository) {

        this.attemptRepository = attemptRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<GamePlayQuestionDTO> getQuestionsForAttempt(
            Long attemptId, String studentEmail) {

        var student = userRepository.findByEmail(studentEmail)
                .filter(user -> user.getRole() == Role.STUDENT)
                .orElseThrow(() ->
                        new IllegalArgumentException("Student account not found."));

        GameAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Game attempt not found."));

        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw new IllegalArgumentException(
                    "You are not allowed to access this game attempt.");
        }

        Long gameId = attempt.getAssignment().getGame().getId();

        List<GameQuestion> questions =
                questionRepository.findByGameIdOrderByQuestionOrderAsc(gameId);

        var savedAnswers = answerRepository
                .findByAttempt_IdOrderByQuestion_QuestionOrderAsc(attemptId)
                .stream()
                .filter(answer -> answer.getStudentAnswer() != null)
                .collect(java.util.stream.Collectors.toMap(
                        answer -> answer.getQuestion().getId(),
                        answer -> answer));

        boolean matchingGame = attempt.getAssignment().getGame().getGameType()
                == GameType.MATCHING;
        List<String> matchingOptions = List.of();
        if (matchingGame) {
            List<String> options = questions.stream()
                    .map(GameQuestion::getRightItem)
                    .filter(option -> option != null && !option.isBlank())
                    .distinct()
                    .collect(java.util.stream.Collectors.toCollection(
                            ArrayList::new));
            Collections.shuffle(options);
            matchingOptions = List.copyOf(options);
        }

        List<String> availableMatchingOptions = matchingOptions;

        return questions.stream()
                .map(question -> {
                    GameAttemptAnswer savedAnswer =
                            savedAnswers.get(question.getId());
                    return new GamePlayQuestionDTO(
                            question,
                            savedAnswer == null
                                    ? null : savedAnswer.getStudentAnswer(),
                            savedAnswer != null && savedAnswer.isEvaluated()
                                    && Boolean.TRUE.equals(savedAnswer.getCorrect()),
                            savedAnswer != null && savedAnswer.isEvaluated(),
                            availableMatchingOptions);
                })
                .toList();
    }
}
