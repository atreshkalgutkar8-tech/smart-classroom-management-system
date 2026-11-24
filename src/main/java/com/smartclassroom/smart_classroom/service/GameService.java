
package com.smartclassroom.smart_classroom.service;

import com.smartclassroom.smart_classroom.entity.*;
import com.smartclassroom.smart_classroom.repository.GameRepository;
import com.smartclassroom.smart_classroom.repository.GameQuestionRepository;
import com.smartclassroom.smart_classroom.repository.GameAssignmentRepository;
import com.smartclassroom.smart_classroom.repository.GameAttemptRepository;
import com.smartclassroom.smart_classroom.repository.GameAttemptAnswerRepository;
import com.smartclassroom.smart_classroom.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class GameService {

    private final GameRepository gameRepository;
    private final GameQuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final GameAssignmentRepository assignmentRepository;
    private final GameAttemptRepository attemptRepository;
    private final GameAttemptAnswerRepository answerRepository;

    public GameService(
            GameRepository gameRepository,
            GameQuestionRepository questionRepository,
            UserRepository userRepository,
            GameAssignmentRepository assignmentRepository,
            GameAttemptRepository attemptRepository,
            GameAttemptAnswerRepository answerRepository) {

        this.gameRepository = gameRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
    }

    // Validate grade and subject
    private void validateGameDetails(
            String title,
            String description,
            String subject,
            Integer grade,
            GameType gameType,
            PuzzleTemplate puzzleTemplate,
            MatchingFormat matchingFormat) {

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Game title is required.");
        }

        if (title.trim().length() > 150) {
            throw new IllegalArgumentException(
                    "Game title cannot exceed 150 characters.");
        }

        if (description != null && description.trim().length() > 1000) {
            throw new IllegalArgumentException(
                    "Description cannot exceed 1000 characters.");
        }

        if (grade == null || grade < 1 || grade > 5) {
            throw new IllegalArgumentException(
                    "Grade must be between 1 and 5.");
        }

        List<String> validSubjects = List.of(
                "Mathematics",
                "Kannada",
                "English",
                "EVS"
        );

        if (subject == null || !validSubjects.contains(subject.trim())) {
            throw new IllegalArgumentException(
                    "Select a valid subject.");
        }

        if (gameType == null) {
            throw new IllegalArgumentException(
                    "Select a game type.");
        }

        if (gameType == GameType.PUZZLE && puzzleTemplate == null) {
            throw new IllegalArgumentException(
                    "Select a puzzle template.");
        }

        if (gameType == GameType.MATCHING && matchingFormat == null) {
            throw new IllegalArgumentException(
                    "Select a matching format.");
        }

        if (gameType != GameType.PUZZLE && puzzleTemplate != null) {
            throw new IllegalArgumentException(
                    "Puzzle template is only valid for puzzle games.");
        }

        if (gameType != GameType.MATCHING && matchingFormat != null) {
            throw new IllegalArgumentException(
                    "Matching format is only valid for matching games.");
        }
    }

    // Create a custom game
    @Transactional
    public Game createCustomGame(
            String teacherEmail,
            String title,
            String description,
            GameType gameType,
            String subject,
            Integer grade,
            PuzzleTemplate puzzleTemplate,
            MatchingFormat matchingFormat) {

        return createCustomGame(
                teacherEmail, title, description, gameType, subject, grade,
                puzzleTemplate, matchingFormat,
                GameVisibility.AVAILABLE_TO_STUDENTS);
    }

    @Transactional
    public Game createCustomGame(
            String teacherEmail,
            String title,
            String description,
            GameType gameType,
            String subject,
            Integer grade,
            PuzzleTemplate puzzleTemplate,
            MatchingFormat matchingFormat,
            GameVisibility visibility) {

        validateGameDetails(
                title,
                description,
                subject,
                grade,
                gameType,
                puzzleTemplate,
                matchingFormat
        );

        if (gameType == GameType.PUZZLE) {
            validateSupportedPuzzleTemplate(puzzleTemplate);
        }

        if (visibility == null) {
            throw new IllegalArgumentException("Select a valid visibility option.");
        }

        User teacher = findTeacher(teacherEmail);

        Game game = new Game();

        game.setTitle(title.trim());
        game.setDescription(description == null || description.isBlank()
                ? null : description.trim());
        game.setGameType(gameType);
        game.setSubject(subject.trim());
        game.setGrade(grade);
        game.setSource(GameSource.CUSTOM);
        game.setCreatedBy(teacher);
        game.setVisibility(visibility);

        game.setPuzzleTemplate(puzzleTemplate);
        game.setMatchingFormat(matchingFormat);

        return gameRepository.save(game);
    }

    @Transactional(readOnly = true)
    public List<Game> getPreloadedGames() {
        return gameRepository.findBySourceOrderByGradeAscTitleAsc(GameSource.PRELOADED);
    }

    @Transactional(readOnly = true)
    public List<Game> getCustomGamesForTeacher(String teacherEmail) {
        User teacher = findTeacher(teacherEmail);
        return gameRepository.findBySourceAndCreatedByIdOrderByGradeAscTitleAsc(
                GameSource.CUSTOM, teacher.getId());
    }

    @Transactional(readOnly = true)
    public Game getOwnedCustomGameForTeacher(Long gameId, String teacherEmail) {
        return getOwnedCustomGame(gameId, teacherEmail);
    }

    @Transactional
    public Game updateCustomGame(
            Long gameId, String teacherEmail, String title, String description,
            String subject, Integer grade, GameType gameType,
            PuzzleTemplate puzzleTemplate, MatchingFormat matchingFormat,
            GameVisibility visibility) {

        Game game = getOwnedCustomGame(gameId, teacherEmail);
        validateGameDetails(title, description, subject, grade,
                gameType, puzzleTemplate, matchingFormat);
        if (gameType == GameType.PUZZLE
                && puzzleTemplate != game.getPuzzleTemplate()) {
            validateSupportedPuzzleTemplate(puzzleTemplate);
        }
        if (visibility == null) {
            throw new IllegalArgumentException("Select a valid visibility option.");
        }
        boolean assigned = assignmentRepository.countByGame_Id(gameId) > 0;
        boolean questionStructureChanged = game.getGameType() != gameType
                || game.getPuzzleTemplate() != puzzleTemplate
                || game.getMatchingFormat() != matchingFormat;
        if (assigned && (questionStructureChanged || !game.getGrade().equals(grade))) {
            throw new IllegalArgumentException(
                    "Game type, template, format, and grade cannot change after assignment.");
        }
        if (game.getGameType() == GameType.PUZZLE && !game.getGrade().equals(grade)
                && questionRepository.countByGame_Id(gameId) > 0) {
            throw new IllegalArgumentException("Puzzle grade cannot change after questions are added.");
        }
        if (questionStructureChanged && questionRepository.countByGame_Id(gameId) > 0) {
            throw new IllegalArgumentException(
                    "Game type, template, or format cannot change after questions are added.");
        }

        game.setTitle(title.trim());
        game.setDescription(description == null || description.isBlank()
                ? null : description.trim());
        game.setSubject(subject.trim());
        game.setGrade(grade);
        game.setGameType(gameType);
        game.setPuzzleTemplate(puzzleTemplate);
        game.setMatchingFormat(matchingFormat);
        game.setVisibility(visibility);

        return gameRepository.save(game);
    }

    // Get preloaded games and the logged-in teacher's own games
    @Transactional(readOnly = true)
    public List<Game> getGamesForTeacher(
            String teacherEmail,
            Integer grade) {

        User teacher = userRepository
                .findByEmail(teacherEmail.trim().toLowerCase())
                .filter(user -> user.getRole() == Role.TEACHER)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Teacher account not found."
                ));

        if (grade == null || grade < 1 || grade > 5) {
            throw new IllegalArgumentException(
                    "Grade must be between 1 and 5.");
        }

        List<Game> games = new ArrayList<>();

        // Shared preloaded games
        games.addAll(
                gameRepository.findBySourceAndGradeAndVisibility(
                        GameSource.PRELOADED,
                        grade,
                        GameVisibility.AVAILABLE_TO_STUDENTS
                )
        );

        // Only this teacher's custom games
        games.addAll(
                gameRepository.findBySourceAndCreatedByIdAndGradeAndVisibility(
                        GameSource.CUSTOM,
                        teacher.getId(),
                        grade,
                        GameVisibility.AVAILABLE_TO_STUDENTS
                )
        );

        return games.stream()
                .filter(game -> game.getGameType() != GameType.PUZZLE
                        || PuzzleConfiguration.isSupportedGame(questionRepository
                                .findByGameIdOrderByQuestionOrderAsc(game.getId())))
                .toList();
    }

    // Update visibility of a teacher's own custom game
    @Transactional
    public Game updateGameVisibility(
            Long gameId,
            String teacherEmail,
            GameVisibility visibility) {

        if (visibility == null) {
            throw new IllegalArgumentException(
                    "Select a valid visibility option.");
        }

        Game game = getOwnedCustomGame(gameId, teacherEmail);

        game.setVisibility(visibility);

        return gameRepository.save(game);
    }

    // Delete a teacher's own custom game and its questions
    @Transactional
    public void deleteCustomGame(
            Long gameId,
            String teacherEmail) {

        Game game = getOwnedCustomGame(gameId, teacherEmail);

        List<Long> assignmentIds = assignmentRepository.findByGame_Id(gameId)
                .stream().map(assignment -> assignment.getId()).toList();
        if (!assignmentIds.isEmpty()) {
            List<com.smartclassroom.smart_classroom.entity.GameAttempt> attempts =
                    attemptRepository.findByAssignment_IdIn(assignmentIds);
            for (var attempt : attempts) {
                answerRepository.deleteByAttempt_Id(attempt.getId());
            }
            answerRepository.flush();
            attemptRepository.deleteAll(attempts);
            attemptRepository.flush();
        }

        assignmentRepository.deleteByGame_Id(gameId);
        assignmentRepository.flush();

        questionRepository.deleteByGameId(gameId);
        questionRepository.flush();

        gameRepository.delete(game);
        gameRepository.flush();
    }

    // Check ownership and prevent editing another teacher's game
    private Game getOwnedCustomGame(
            Long gameId,
            String teacherEmail) {

        User teacher = findTeacher(teacherEmail);

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Game not found."
                ));

        if (game.getSource() != GameSource.CUSTOM) {
            throw new IllegalArgumentException(
                    "Preloaded games cannot be modified or deleted.");
        }

        if (game.getCreatedBy() == null ||
                !game.getCreatedBy().getId().equals(teacher.getId())) {

            throw new IllegalArgumentException(
                    "You cannot modify another teacher's game.");
        }

        return game;
    }

    @Transactional
    public Game duplicateCustomGame(Long gameId, String teacherEmail) {
        Game source = getOwnedCustomGame(gameId, teacherEmail);
        Game copy = new Game();
        String copyBaseTitle = source.getTitle().length() > 143
                ? source.getTitle().substring(0, 143) : source.getTitle();
        copy.setTitle(copyBaseTitle + " (Copy)");
        copy.setDescription(source.getDescription());
        copy.setGameType(source.getGameType());
        copy.setSubject(source.getSubject());
        copy.setGrade(source.getGrade());
        copy.setSource(GameSource.CUSTOM);
        copy.setCreatedBy(source.getCreatedBy());
        copy.setVisibility(source.getVisibility());
        copy.setPuzzleTemplate(source.getPuzzleTemplate());
        copy.setMatchingFormat(source.getMatchingFormat());
        Game savedCopy = gameRepository.save(copy);

        for (GameQuestion original : questionRepository
                .findByGameIdOrderByQuestionOrderAsc(source.getId())) {
            GameQuestion question = new GameQuestion();
            question.setGame(savedCopy);
            question.setQuestionText(original.getQuestionText());
            question.setOptionA(original.getOptionA());
            question.setOptionB(original.getOptionB());
            question.setOptionC(original.getOptionC());
            question.setOptionD(original.getOptionD());
            question.setCorrectAnswer(original.getCorrectAnswer());
            question.setLeftItem(original.getLeftItem());
            question.setRightItem(original.getRightItem());
            question.setLeftImageUrl(original.getLeftImageUrl());
            question.setRightImageUrl(original.getRightImageUrl());
            question.setPuzzleData(original.getPuzzleData());
            question.setQuestionOrder(original.getQuestionOrder());
            questionRepository.save(question);
        }
        return savedCopy;
    }

    private User findTeacher(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(user -> user.getRole() == Role.TEACHER)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Teacher account not found."));
    }

    private void validateSupportedPuzzleTemplate(PuzzleTemplate template) {
        if (template != PuzzleTemplate.WORD && template != PuzzleTemplate.NUMBER) {
            throw new IllegalArgumentException(
                    "New puzzle games can use Word or Number templates only.");
        }
    }
}
