
package com.smartclassroom.smart_classroom.service;

import com.smartclassroom.smart_classroom.entity.*;
import com.smartclassroom.smart_classroom.repository.GameRepository;
import com.smartclassroom.smart_classroom.repository.GameQuestionRepository;
import com.smartclassroom.smart_classroom.repository.GameAttemptAnswerRepository;
import com.smartclassroom.smart_classroom.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.net.URI;

@Service
public class GameQuestionService {

    private final GameRepository gameRepository;
    private final GameQuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final GameAttemptAnswerRepository answerRepository;

    public GameQuestionService(
            GameRepository gameRepository,
            GameQuestionRepository questionRepository,
            UserRepository userRepository,
            GameAttemptAnswerRepository answerRepository) {

        this.gameRepository = gameRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.answerRepository = answerRepository;
    }

    // Verify that the game belongs to the logged-in teacher
    private Game getOwnedGame(Long gameId, String teacherEmail) {

        User teacher = userRepository
                .findByEmail(teacherEmail.trim().toLowerCase(java.util.Locale.ROOT))
                .filter(user -> user.getRole() == Role.TEACHER)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Teacher account not found."
                ));

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Game not found."
                ));

        if (game.getSource() != GameSource.CUSTOM) {
            throw new IllegalArgumentException(
                    "Preloaded games cannot be modified."
            );
        }

        if (game.getCreatedBy() == null ||
                !game.getCreatedBy().getId().equals(teacher.getId())) {

            throw new IllegalArgumentException(
                    "You cannot modify another teacher's game."
            );
        }

        return game;
    }

    // Validate quiz question
    private void validateQuizQuestion(GameQuestion question) {

        if (question.getQuestionText() == null ||
                question.getQuestionText().isBlank()) {

            throw new IllegalArgumentException(
                    "Question text is required."
            );
        }
        validateLength(question.getQuestionText(), 1000, "Question text");

        if (question.getOptionA() == null ||
                question.getOptionB() == null ||
                question.getOptionC() == null ||
                question.getOptionD() == null) {

            throw new IllegalArgumentException(
                    "All four answer options are required."
            );
        }

        if (question.getCorrectAnswer() == null ||
                question.getCorrectAnswer().isBlank()) {

            throw new IllegalArgumentException(
                    "Correct answer is required."
            );
        }

        List<String> options = List.of(
                question.getOptionA().trim(),
                question.getOptionB().trim(),
                question.getOptionC().trim(),
                question.getOptionD().trim()
        );

        validateLength(question.getOptionA(), 500, "Option");
        validateLength(question.getOptionB(), 500, "Option");
        validateLength(question.getOptionC(), 500, "Option");
        validateLength(question.getOptionD(), 500, "Option");
        validateLength(question.getCorrectAnswer(), 500, "Correct answer");

        if (options.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException(
                    "Answer options cannot be empty."
            );
        }

        if (!options.contains(question.getCorrectAnswer().trim())) {
            throw new IllegalArgumentException(
                    "Correct answer must match one of the four options."
            );
        }
    }

    // Validate matching pair
    private void validateMatchingQuestion(GameQuestion question) {

        if (question.getQuestionText() == null
                || question.getQuestionText().isBlank()) {
            throw new IllegalArgumentException("Question text is required.");
        }

        if (question.getLeftItem() == null ||
                question.getLeftItem().isBlank()) {

            throw new IllegalArgumentException(
                    "Left matching item is required."
            );
        }

        validateLength(question.getQuestionText(), 1000, "Question text");
        validateLength(question.getLeftItem(), 500, "Left item");
        validateLength(question.getRightItem(), 500, "Right item");

        if (question.getRightItem() == null ||
                question.getRightItem().isBlank()) {

            throw new IllegalArgumentException(
                    "Right matching item is required."
            );
        }
    }

    private void validateLength(String value, int max, String label) {
        if (value != null && value.trim().length() > max) {
            throw new IllegalArgumentException(label + " cannot exceed " + max + " characters.");
        }
    }

    private int nextQuestionOrder(Long gameId) {
        return questionRepository.findFirstByGameIdOrderByQuestionOrderDesc(gameId)
                .map(question -> question.getQuestionOrder() + 1)
                .orElse(1);
    }

    @Transactional
    public GameQuestion addQuestion(
            Long gameId, String teacherEmail, GameQuestion question,
            PuzzleTechnique puzzleTechnique) {
        Game game = getOwnedGame(gameId, teacherEmail);
        return switch (game.getGameType()) {
            case QUIZ -> addQuizQuestion(gameId, teacherEmail, question);
            case MATCHING -> addMatchingPair(gameId, teacherEmail, question);
            case PUZZLE -> addPuzzle(gameId, teacherEmail, question, puzzleTechnique);
        };
    }

    // Add a quiz question
    @Transactional
    public GameQuestion addQuizQuestion(
            Long gameId,
            String teacherEmail,
            GameQuestion question) {

        Game game = getOwnedGame(gameId, teacherEmail);

        if (game.getGameType() != GameType.QUIZ) {
            throw new IllegalArgumentException(
                    "This game is not a quiz."
            );
        }

        validateQuizQuestion(question);

        question.setGame(game);
        question.setQuestionOrder(nextQuestionOrder(gameId));

        return questionRepository.save(question);
    }

    // Add a matching pair
    @Transactional
    public GameQuestion addMatchingPair(
            Long gameId,
            String teacherEmail,
            GameQuestion question) {

        Game game = getOwnedGame(gameId, teacherEmail);

        if (game.getGameType() != GameType.MATCHING) {
            throw new IllegalArgumentException(
                    "This game is not a matching game."
            );
        }

        validateMatchingQuestion(question);
        ensureUniqueMatchingPairs(gameId, List.of(question));

        if (game.getMatchingFormat() == MatchingFormat.TEXT_TO_TEXT) {

            if (question.getLeftImageUrl() != null ||
                    question.getRightImageUrl() != null) {

                throw new IllegalArgumentException(
                        "Text-to-text matching cannot contain images."
                );
            }

        } else if (game.getMatchingFormat() ==
                MatchingFormat.IMAGE_TO_TEXT) {

            if (question.getLeftImageUrl() == null ||
                    question.getLeftImageUrl().isBlank()) {

                throw new IllegalArgumentException(
                        "Image URL is required for image-to-text matching."
                );
            }

            validateImageUrl(question.getLeftImageUrl());
            if (question.getRightImageUrl() != null) {
                throw new IllegalArgumentException(
                        "Image-to-text matching uses an image on the left only.");
            }

        }

        question.setGame(game);
        question.setQuestionOrder(nextQuestionOrder(gameId));

        return questionRepository.save(question);
    }

    @Transactional
    public List<GameQuestion> addMatchingPairs(
            Long gameId,
            String teacherEmail,
            String questionText,
            List<String> leftItems,
            List<String> rightItems,
            List<String> leftImageUrls) {

        Game game = getOwnedGame(gameId, teacherEmail);
        if (game.getGameType() != GameType.MATCHING) {
            throw new IllegalArgumentException("This game is not a matching game.");
        }
        if (leftItems == null || rightItems == null || leftItems.isEmpty()
                || leftItems.size() != rightItems.size()) {
            throw new IllegalArgumentException(
                    "Provide a left item and a correct right item for every pair.");
        }

        List<String> imageUrls = leftImageUrls == null
                ? new ArrayList<>() : new ArrayList<>(leftImageUrls);
        if (game.getMatchingFormat() == MatchingFormat.IMAGE_TO_TEXT
                && imageUrls.size() != leftItems.size()) {
            throw new IllegalArgumentException(
                    "Provide an image URL for every image-to-text pair.");
        }
        if (imageUrls.isEmpty()) {
            imageUrls = new ArrayList<>(java.util.Collections.nCopies(
                    leftItems.size(), null));
        }
        if (imageUrls.size() != leftItems.size()) {
            throw new IllegalArgumentException(
                    "Pair fields do not have matching counts.");
        }

        List<GameQuestion> newPairs = new ArrayList<>();
        int nextOrder = nextQuestionOrder(gameId);
        for (int index = 0; index < leftItems.size(); index++) {
            GameQuestion pair = new GameQuestion();
            pair.setQuestionText(questionText);
            pair.setLeftItem(leftItems.get(index));
            pair.setRightItem(rightItems.get(index));
            pair.setLeftImageUrl(imageUrls.get(index));
            pair.setGame(game);
            pair.setQuestionOrder(nextOrder++);

            validateMatchingQuestion(pair);
            if (game.getMatchingFormat() == MatchingFormat.TEXT_TO_TEXT) {
                if (pair.getLeftImageUrl() != null
                        && !pair.getLeftImageUrl().isBlank()) {
                    throw new IllegalArgumentException(
                            "Text-to-text matching cannot contain images.");
                }
                pair.setLeftImageUrl(null);
            } else if (game.getMatchingFormat() == MatchingFormat.IMAGE_TO_TEXT) {
                if (pair.getLeftImageUrl() == null
                        || pair.getLeftImageUrl().isBlank()) {
                    throw new IllegalArgumentException(
                            "Image URL is required for image-to-text matching.");
                }
                validateImageUrl(pair.getLeftImageUrl());
            } else {
                throw new IllegalArgumentException(
                        "A matching format must be selected for this game.");
            }
            newPairs.add(pair);
        }

        ensureUniqueMatchingPairs(gameId, newPairs);
        return questionRepository.saveAll(newPairs);
    }

    private void ensureUniqueMatchingPairs(
            Long gameId, List<GameQuestion> newPairs) {
        Set<String> leftItems = new HashSet<>();
        Set<String> rightItems = new HashSet<>();
        for (GameQuestion existing : questionRepository
                .findByGameIdOrderByQuestionOrderAsc(gameId)) {
            leftItems.add(normalizeMatchValue(existing.getLeftItem()));
            rightItems.add(normalizeMatchValue(existing.getRightItem()));
        }
        for (GameQuestion pair : newPairs) {
            if (!leftItems.add(normalizeMatchValue(pair.getLeftItem()))) {
                throw new IllegalArgumentException(
                        "Each left-side item must be unique in a matching game.");
            }
            if (!rightItems.add(normalizeMatchValue(pair.getRightItem()))) {
                throw new IllegalArgumentException(
                        "Each correct right-side item must be unique in a matching game.");
            }
        }
    }

    private String normalizeMatchValue(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    // Add a puzzle
    @Transactional
    public GameQuestion addPuzzle(
            Long gameId,
            String teacherEmail,
            GameQuestion question,
            PuzzleTechnique technique) {

        Game game = getOwnedGame(gameId, teacherEmail);

        if (game.getGameType() != GameType.PUZZLE) {
            throw new IllegalArgumentException(
                    "This game is not a puzzle."
            );
        }

        PuzzleConfiguration.PuzzleContent content = PuzzleConfiguration.build(
                game.getPuzzleTemplate(), game.getGrade(), technique,
                question.getOptionA(), question.getOptionB(),
                question.getOptionC(), question.getOptionD());
        String instruction = question.getQuestionText() == null || question.getQuestionText().isBlank()
                ? PuzzleConfiguration.defaultInstruction(technique) : question.getQuestionText().trim();
        validateLength(instruction, 1000, "Question instruction");
        question.setQuestionText(instruction);
        question.setCorrectAnswer(content.correctAnswer());
        question.setPuzzleData(content.puzzleData());
        question.setOptionA(null);
        question.setOptionB(null);
        question.setOptionC(null);
        question.setOptionD(null);
        question.setGame(game);
        question.setQuestionOrder(nextQuestionOrder(gameId));

        return questionRepository.save(question);
    }

    @Transactional
    public GameQuestion addPuzzle(Long gameId, String teacherEmail, String instruction,
            PuzzleTechnique technique, String suppliedItems, String expectedSequence,
            String pattern, String missingValues) {
        GameQuestion question = new GameQuestion();
        question.setQuestionText(instruction);
        question.setOptionA(suppliedItems);
        question.setOptionB(expectedSequence);
        question.setOptionC(pattern);
        question.setOptionD(missingValues);
        return addPuzzle(gameId, teacherEmail, question, technique);
    }

    @Transactional
    public GameQuestion updatePuzzle(Long gameId, Long questionId, String teacherEmail,
            String instruction, PuzzleTechnique technique, String suppliedItems,
            String expectedSequence, String pattern, String missingValues) {
        Game game = getOwnedGame(gameId, teacherEmail);
        if (game.getGameType() != GameType.PUZZLE) throw new IllegalArgumentException("This game is not a puzzle.");
        GameQuestion existing = questionRepository.findById(questionId)
                .filter(q -> q.getGame().getId().equals(gameId))
                .orElseThrow(() -> new IllegalArgumentException("Question not found in this game."));
        PuzzleConfiguration.PuzzleContent content = PuzzleConfiguration.build(
                game.getPuzzleTemplate(), game.getGrade(), technique,
                suppliedItems, expectedSequence, pattern, missingValues);
        String text = instruction == null || instruction.isBlank()
                ? PuzzleConfiguration.defaultInstruction(technique) : instruction.trim();
        validateLength(text, 1000, "Question instruction");
        existing.setQuestionText(text);
        existing.setCorrectAnswer(content.correctAnswer());
        existing.setPuzzleData(content.puzzleData());
        existing.setOptionA(null);
        existing.setOptionB(null);
        existing.setOptionC(null);
        existing.setOptionD(null);
        return questionRepository.save(existing);
    }

    // Get questions for a teacher's own game
    @Transactional(readOnly = true)
    public List<GameQuestion> getQuestionsForTeacher(
            Long gameId,
            String teacherEmail) {

        getOwnedGame(gameId, teacherEmail);

        return questionRepository
                .findByGameIdOrderByQuestionOrderAsc(gameId);
    }

    // Edit an existing question
    @Transactional
    public GameQuestion updateQuestion(
            Long gameId,
            Long questionId,
            String teacherEmail,
            GameQuestion updatedQuestion,
            PuzzleTechnique puzzleTechnique) {

        Game game = getOwnedGame(gameId, teacherEmail);

        GameQuestion existingQuestion = questionRepository
                .findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Question not found."
                ));

        if (!existingQuestion.getGame().getId().equals(gameId)) {
            throw new IllegalArgumentException(
                    "This question does not belong to the selected game."
            );
        }

        existingQuestion.setQuestionText(
                updatedQuestion.getQuestionText()
        );

        existingQuestion.setOptionA(updatedQuestion.getOptionA());
        existingQuestion.setOptionB(updatedQuestion.getOptionB());
        existingQuestion.setOptionC(updatedQuestion.getOptionC());
        existingQuestion.setOptionD(updatedQuestion.getOptionD());
        existingQuestion.setCorrectAnswer(
                updatedQuestion.getCorrectAnswer()
        );

        existingQuestion.setLeftItem(updatedQuestion.getLeftItem());
        existingQuestion.setRightItem(updatedQuestion.getRightItem());
        existingQuestion.setLeftImageUrl(
                updatedQuestion.getLeftImageUrl()
        );
        existingQuestion.setRightImageUrl(
                updatedQuestion.getRightImageUrl()
        );
        existingQuestion.setPuzzleData(updatedQuestion.getPuzzleData());

        // Validate updated content based on game type
        if (game.getGameType() == GameType.QUIZ) {
            validateQuizQuestion(existingQuestion);

        } else if (game.getGameType() == GameType.MATCHING) {
            validateMatchingQuestion(existingQuestion);
            if (game.getMatchingFormat() == MatchingFormat.TEXT_TO_TEXT) {
                if (existingQuestion.getLeftImageUrl() != null
                        || existingQuestion.getRightImageUrl() != null) {
                    throw new IllegalArgumentException(
                            "Text-to-text matching cannot contain images.");
                }
            } else {
                if (existingQuestion.getLeftImageUrl() == null
                        || existingQuestion.getLeftImageUrl().isBlank()) {
                    throw new IllegalArgumentException(
                            "Image URL is required for image-to-text matching.");
                }
                validateImageUrl(existingQuestion.getLeftImageUrl());
                if (existingQuestion.getRightImageUrl() != null) {
                    throw new IllegalArgumentException(
                            "Image-to-text matching uses an image on the left only.");
                }
            }
            ensureMatchingPairUniqueAfterEdit(gameId, existingQuestion);

        } else if (game.getGameType() == GameType.PUZZLE) {
            throw new IllegalArgumentException("Use the puzzle question editor to edit puzzle questions.");
        }

        validateLength(existingQuestion.getQuestionText(), 1000, "Question text");

        return questionRepository.save(existingQuestion);
    }

    private void ensureMatchingPairUniqueAfterEdit(
            Long gameId, GameQuestion updatedPair) {
        String updatedLeft = normalizeMatchValue(updatedPair.getLeftItem());
        String updatedRight = normalizeMatchValue(updatedPair.getRightItem());
        for (GameQuestion pair : questionRepository
                .findByGameIdOrderByQuestionOrderAsc(gameId)) {
            if (pair.getId().equals(updatedPair.getId())) {
                continue;
            }
            if (normalizeMatchValue(pair.getLeftItem()).equals(updatedLeft)) {
                throw new IllegalArgumentException(
                        "Each left-side item must be unique in a matching game.");
            }
            if (normalizeMatchValue(pair.getRightItem()).equals(updatedRight)) {
                throw new IllegalArgumentException(
                        "Each correct right-side item must be unique in a matching game.");
            }
        }
    }

    // Delete an individual question
    @Transactional
    public void deleteQuestion(
            Long gameId,
            Long questionId,
            String teacherEmail) {

        getOwnedGame(gameId, teacherEmail);

        GameQuestion question = questionRepository
                .findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Question not found."
                ));

        if (!question.getGame().getId().equals(gameId)) {
            throw new IllegalArgumentException(
                    "This question does not belong to the selected game."
            );
        }

        if (answerRepository.countByQuestion_Id(questionId) > 0) {
            throw new IllegalArgumentException(
                    "This question has saved student answers and cannot be deleted individually.");
        }

        questionRepository.delete(question);
        List<GameQuestion> remaining = questionRepository
                .findByGameIdOrderByQuestionOrderAsc(gameId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setQuestionOrder(i + 1);
        }
    }

    private void validateImageUrl(String imageUrl) {
        String normalized = imageUrl.trim().toLowerCase(java.util.Locale.ROOT);
        boolean validUrl = normalized.startsWith("https://")
                || normalized.startsWith("http://");
        if (validUrl) {
            try {
                validUrl = URI.create(imageUrl.trim()).getHost() != null;
            } catch (IllegalArgumentException e) {
                validUrl = false;
            }
        }
        boolean validLocalPath = normalized.startsWith("/images/")
                && normalized.length() > "/images/".length()
                && !normalized.contains("..")
                && !normalized.contains("?")
                && !normalized.contains("#");
        if (!validUrl && !validLocalPath) {
            throw new IllegalArgumentException(
                    "Image URL must use HTTP, HTTPS, or a local /images/ path.");
        }
        validateLength(imageUrl, 1000, "Image URL");
    }
}
