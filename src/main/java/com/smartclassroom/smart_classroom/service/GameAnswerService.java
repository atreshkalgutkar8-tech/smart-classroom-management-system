
package com.smartclassroom.smart_classroom.service;

import com.smartclassroom.smart_classroom.entity.*;
import com.smartclassroom.smart_classroom.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class GameAnswerService {

    private final GameAttemptRepository attemptRepository;
    private final GameAttemptAnswerRepository answerRepository;
    private final GameQuestionRepository questionRepository;
    private final UserRepository userRepository;

    public GameAnswerService(
            GameAttemptRepository attemptRepository,
            GameAttemptAnswerRepository answerRepository,
            GameQuestionRepository questionRepository,
            UserRepository userRepository) {

        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
    }

    // Save or update a student's answer
    @Transactional
    public GameAttemptAnswer saveAnswer(
            Long attemptId,
            Long questionId,
            String studentAnswer,
            String studentEmail) {

        GameAttempt attempt = getStudentAttempt(attemptId, studentEmail);

        if (Boolean.TRUE.equals(attempt.getCompleted())) {
            throw new IllegalArgumentException(
                    "This game attempt has already been completed.");
        }

        if (attempt.getAssignment().getGame().getGameType() == GameType.MATCHING) {
            throw new IllegalArgumentException(
                    "Submit the complete matching board together.");
        }

        GameQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Question not found."));

        if (!question.getGame().getId()
                .equals(attempt.getAssignment().getGame().getId())) {
            throw new IllegalArgumentException(
                    "This question does not belong to the assigned game.");
        }

        if (question.getGame().getGameType() == GameType.PUZZLE) {
            String draft = PuzzleConfiguration.validateSubmission(question, studentAnswer, true);
            GameAttemptAnswer answer = answerRepository
                    .findByAttempt_IdAndQuestion_Id(attemptId, questionId)
                    .orElseGet(GameAttemptAnswer::new);
            answer.setAttempt(attempt);
            answer.setQuestion(question);
            answer.setStudentAnswer(draft);
            answer.setEvaluated(false);
            answer.setCorrect(false);
            if (draftComplete(question, draft)) {
                answer.setCorrect(PuzzleConfiguration.isCorrect(question, draft));
                answer.setEvaluated(true);
            }
            GameAttemptAnswer saved = answerRepository.save(answer);
            updateScore(attempt);
            return saved;
        }

        if (studentAnswer == null || studentAnswer.isBlank()) {
            throw new IllegalArgumentException("Please provide an answer.");
        }

        String submittedAnswer = studentAnswer.trim();

        // Check answer on the server
        boolean isCorrect = checkAnswer(question, submittedAnswer);

        // Update existing answer or create a new one
        GameAttemptAnswer answer = answerRepository
                .findByAttempt_IdAndQuestion_Id(attemptId, questionId)
                .orElseGet(GameAttemptAnswer::new);

        answer.setAttempt(attempt);
        answer.setQuestion(question);
        answer.setStudentAnswer(submittedAnswer);
        answer.setCorrect(isCorrect);
        answer.setEvaluated(true);

        GameAttemptAnswer savedAnswer = answerRepository.save(answer);

        // Update current score
        updateScore(attempt);

        return savedAnswer;
    }

    @Transactional
    public void saveMatchingDraft(
            Long assignmentId,
            Long attemptId,
            List<Long> questionIds,
            List<String> selectedRightItems,
            String studentEmail) {
        GameAttempt attempt = getStudentAttempt(attemptId, studentEmail);
        validateAssignmentAndMatchingGame(attempt, assignmentId);
        Map<Long, String> selections = validateMatchingBoard(
                attempt, questionIds, selectedRightItems, true);

        List<GameAttemptAnswer> answersToSave = new ArrayList<>();
        for (Map.Entry<Long, String> selection : selections.entrySet()) {
            GameQuestion question = questionRepository.findById(selection.getKey())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Matching item not found."));
            GameAttemptAnswer answer = answerRepository
                    .findByAttempt_IdAndQuestion_Id(attemptId, question.getId())
                    .orElse(null);
            if (answer == null && selection.getValue() == null) {
                continue;
            }
            if (answer == null) {
                answer = new GameAttemptAnswer();
            }
            answer.setAttempt(attempt);
            answer.setQuestion(question);
            answer.setStudentAnswer(selection.getValue());
            answer.setCorrect(false);
            answer.setEvaluated(false);
            answersToSave.add(answer);
        }
        answerRepository.saveAll(answersToSave);
        updateScore(attempt);
    }

    @Transactional
    public int submitMatchingBoard(
            Long assignmentId,
            Long attemptId,
            List<Long> questionIds,
            List<String> selectedRightItems,
            String studentEmail) {
        GameAttempt attempt = getStudentAttempt(attemptId, studentEmail);
        validateAssignmentAndMatchingGame(attempt, assignmentId);
        Map<Long, String> selections = validateMatchingBoard(
                attempt, questionIds, selectedRightItems, false);

        Map<Long, GameQuestion> questionsById = questionRepository
                .findByGameIdOrderByQuestionOrderAsc(
                        attempt.getAssignment().getGame().getId())
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        GameQuestion::getId, question -> question));

        List<GameAttemptAnswer> answersToSave = new ArrayList<>();
        for (Map.Entry<Long, String> selection : selections.entrySet()) {
            GameQuestion question = questionsById.get(selection.getKey());
            GameAttemptAnswer answer = answerRepository
                    .findByAttempt_IdAndQuestion_Id(attemptId, question.getId())
                    .orElseGet(GameAttemptAnswer::new);
            answer.setAttempt(attempt);
            answer.setQuestion(question);
            answer.setStudentAnswer(selection.getValue());
            answer.setCorrect(matches(selection.getValue(), question.getRightItem()));
            answer.setEvaluated(true);
            answersToSave.add(answer);
        }
        answerRepository.saveAll(answersToSave);
        attempt.setTotalQuestions(selections.size());
        updateScore(attempt);
        return attempt.getScore();
    }

    private void validateAssignmentAndMatchingGame(
            GameAttempt attempt, Long assignmentId) {
        if (!attempt.getAssignment().getId().equals(assignmentId)) {
            throw new IllegalArgumentException(
                    "This attempt does not belong to the requested assignment.");
        }
        if (attempt.getAssignment().getGame().getGameType() != GameType.MATCHING) {
            throw new IllegalArgumentException("This is not a matching game.");
        }
    }

    private Map<Long, String> validateMatchingBoard(
            GameAttempt attempt,
            List<Long> questionIds,
            List<String> selectedRightItems,
            boolean allowIncomplete) {
        if (Boolean.TRUE.equals(attempt.getCompleted())) {
            throw new IllegalArgumentException(
                    "This game attempt has already been completed.");
        }

        List<GameQuestion> gameQuestions = questionRepository
                .findByGameIdOrderByQuestionOrderAsc(
                        attempt.getAssignment().getGame().getId());
        if (questionIds == null || selectedRightItems == null
                || questionIds.size() != selectedRightItems.size()
                || questionIds.size() != gameQuestions.size()) {
            throw new IllegalArgumentException(
                    "Submit one selection for every item in this matching board.");
        }

        Map<Long, GameQuestion> questionsById = new HashMap<>();
        Set<String> availableRightItems = new HashSet<>();
        for (GameQuestion question : gameQuestions) {
            questionsById.put(question.getId(), question);
            availableRightItems.add(normalizeMatch(question.getRightItem()));
        }

        Set<Long> seenQuestionIds = new HashSet<>();
        Set<String> seenRightItems = new HashSet<>();
        Map<Long, String> selections = new LinkedHashMap<>();
        for (int index = 0; index < questionIds.size(); index++) {
            Long questionId = questionIds.get(index);
            if (questionId == null || !questionsById.containsKey(questionId)
                    || !seenQuestionIds.add(questionId)) {
                throw new IllegalArgumentException(
                        "Every left-side item must appear exactly once.");
            }

            String requested = selectedRightItems.get(index);
            if (requested == null || requested.isBlank()) {
                if (!allowIncomplete) {
                    throw new IllegalArgumentException(
                            "Match every left-side item before submitting answers.");
                }
                selections.put(questionId, null);
                continue;
            }

            String normalized = normalizeMatch(requested);
            if (!availableRightItems.contains(normalized)) {
                throw new IllegalArgumentException(
                        "A selected right-side item does not belong to this game.");
            }
            if (!seenRightItems.add(normalized)) {
                throw new IllegalArgumentException(
                        "Each right-side item can be used only once.");
            }
            String canonical = gameQuestions.stream()
                    .map(GameQuestion::getRightItem)
                    .filter(value -> matches(requested, value))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "A selected right-side item does not belong to this game."));
            selections.put(questionId, canonical);
        }
        if (!seenQuestionIds.equals(questionsById.keySet())) {
            throw new IllegalArgumentException(
                    "Every left-side item must appear exactly once.");
        }
        return selections;
    }

    private String normalizeMatch(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    // Validate answers according to game type
    private boolean checkAnswer(
            GameQuestion question,
            String submittedAnswer) {

        GameType gameType = question.getGame().getGameType();

        if (gameType == GameType.QUIZ) {

            return matches(
                    submittedAnswer,
                    question.getCorrectAnswer()
            );

        } else if (gameType == GameType.PUZZLE) {
            return PuzzleConfiguration.isCorrect(question, submittedAnswer);
        } else {
            throw new IllegalArgumentException(
                    "This puzzle type is not supported for student play.");
        }
    }

    private boolean draftComplete(GameQuestion question, String submitted) {
        try {
            PuzzleConfiguration.validateSubmission(question, submitted, false);
            return true;
        } catch (IllegalArgumentException incomplete) {
            return false;
        }
    }

    // Case-insensitive answer comparison
    private boolean matches(String submitted, String correct) {

        if (submitted == null || correct == null) {
            return false;
        }

        return submitted.trim().equalsIgnoreCase(correct.trim());
    }

    // Recalculate score from all saved answers
    private void updateScore(GameAttempt attempt) {

        List<GameAttemptAnswer> answers =
                answerRepository
                        .findByAttempt_IdOrderByQuestion_QuestionOrderAsc(
                                attempt.getId());

        int score = (int) answers.stream()
                .filter(GameAttemptAnswer::isEvaluated)
                .filter(answer -> Boolean.TRUE.equals(answer.getCorrect()))
                .count();

        attempt.setScore(score);

        attemptRepository.save(attempt);
    }

    // Submit and complete an attempt
    @Transactional
    public GameAttempt completeAttempt(
            Long attemptId,
            String studentEmail) {

        GameAttempt attempt = getStudentAttempt(attemptId, studentEmail);

        if (Boolean.TRUE.equals(attempt.getCompleted())) {
            return attempt;
        }

        if (attempt.getAssignment().getGame().getGameType()
                == GameType.MATCHING) {
            long evaluatedAnswers = answerRepository
                    .countByAttempt_IdAndEvaluatedTrue(attemptId);
            if (evaluatedAnswers < attempt.getTotalQuestions()) {
                throw new IllegalArgumentException(
                        "Submit the complete matching board before completing the game.");
            }
        }

        if (attempt.getAssignment().getGame().getGameType() == GameType.PUZZLE) {
            List<GameQuestion> playableQuestions = questionRepository
                    .findByGameIdOrderByQuestionOrderAsc(attempt.getAssignment().getGame().getId())
                    .stream().filter(PuzzleConfiguration::isSupportedQuestion).toList();
            if (playableQuestions.isEmpty() || playableQuestions.size() != questionRepository
                    .countByGameId(attempt.getAssignment().getGame().getId())) {
                throw new IllegalArgumentException("This legacy puzzle is not available for student play.");
            }
            int puzzleQuestionCount = playableQuestions.size();
            long evaluatedAnswers = answerRepository
                    .countByAttempt_IdAndEvaluatedTrue(attemptId);
            if (evaluatedAnswers < puzzleQuestionCount) {
                throw new IllegalArgumentException(
                        "Save an answer for every puzzle question before completing the game.");
            }
            attempt.setTotalQuestions(puzzleQuestionCount);
        }

        updateScore(attempt);

        LocalDateTime completedAt = LocalDateTime.now();

        attempt.setCompleted(true);
        attempt.setCompletedAt(completedAt);
        attempt.setLate(completedAt.isAfter(attempt.getAssignment().getDeadline()));

        long seconds = Duration.between(
                attempt.getStartedAt(),
                completedAt
        ).getSeconds();

        attempt.setTimeTakenSeconds((int) Math.max(0, seconds));

        return attemptRepository.save(attempt);
    }

    // Verify that the attempt belongs to the logged-in student
    private GameAttempt getStudentAttempt(
            Long attemptId,
            String studentEmail) {

        User student = userRepository.findByEmail(studentEmail)
                .filter(user -> user.getRole() == Role.STUDENT)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Student account not found."));

        GameAttempt attempt = attemptRepository.findByIdForUpdateNative(attemptId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Game attempt not found."));

        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw new IllegalArgumentException(
                    "You are not authorized to access this attempt.");
        }

        if (!attempt.getAssignment().isActive()) {
            throw new IllegalArgumentException(
                    "This game assignment has been removed.");
        }

        return attempt;
    }
}
