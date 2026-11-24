package com.smartclassroom.smart_classroom.controller;

import com.smartclassroom.smart_classroom.entity.*;
import com.smartclassroom.smart_classroom.service.GameQuestionService;
import com.smartclassroom.smart_classroom.service.GameService;
import com.smartclassroom.smart_classroom.service.PuzzleConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/teacher/games")
public class TeacherGameController {

    private static final List<String> SUBJECTS =
            List.of("Mathematics", "Kannada", "English", "EVS");

    private final GameService gameService;
    private final GameQuestionService questionService;

    public TeacherGameController(
            GameService gameService,
            GameQuestionService questionService) {
        this.gameService = gameService;
        this.questionService = questionService;
    }

    @GetMapping
    public String listGames(Authentication authentication, Model model) {
        model.addAttribute("preloadedGames", gameService.getPreloadedGames()
                .stream().map(GameView::from).toList());
        model.addAttribute("customGames", gameService
                .getCustomGamesForTeacher(authentication.getName())
                .stream().map(GameView::from).toList());
        addOptions(model);
        return "teacher-games";
    }

    @PostMapping("/create")
    public String createGame(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam GameType gameType,
            @RequestParam String subject,
            @RequestParam Integer grade,
            @RequestParam(required = false) PuzzleTemplate puzzleTemplate,
            @RequestParam(required = false) MatchingFormat matchingFormat,
            @RequestParam GameVisibility visibility,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Game createdGame = gameService.createCustomGame(authentication.getName(), title,
                    description, gameType, subject, grade, puzzleTemplate,
                    matchingFormat, visibility);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Custom game created. Add its questions below.");
            return "redirect:/teacher/games/" + createdGame.getId() + "/manage";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/teacher/games";
        }
    }

    @GetMapping("/{gameId}/manage")
    public String manageGame(
            @PathVariable Long gameId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Game game = gameService.getOwnedCustomGameForTeacher(
                    gameId, authentication.getName());
            model.addAttribute("game", GameView.from(game));
            model.addAttribute("legacyPuzzleTemplate",
                    game.getPuzzleTemplate() != null
                            && game.getPuzzleTemplate() != PuzzleTemplate.WORD
                            && game.getPuzzleTemplate() != PuzzleTemplate.NUMBER
                            ? game.getPuzzleTemplate() : null);
            model.addAttribute("questions", questionService
                    .getQuestionsForTeacher(gameId, authentication.getName())
                    .stream().map(QuestionView::from).toList());
            addOptions(model);
            model.addAttribute("puzzleTechniques", puzzleTechniquesFor(game));
            return "teacher-game-manage";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/teacher/games";
        }
    }

    @PostMapping("/{gameId}/update")
    public String updateGame(
            @PathVariable Long gameId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String subject,
            @RequestParam Integer grade,
            @RequestParam GameType gameType,
            @RequestParam(required = false) PuzzleTemplate puzzleTemplate,
            @RequestParam(required = false) MatchingFormat matchingFormat,
            @RequestParam GameVisibility visibility,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            gameService.updateCustomGame(gameId, authentication.getName(), title,
                    description, subject, grade, gameType, puzzleTemplate,
                    matchingFormat, visibility);
            redirectAttributes.addFlashAttribute("successMessage", "Game updated.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/games/" + gameId + "/manage";
    }

    @PostMapping("/{gameId}/visibility")
    public String updateVisibility(
            @PathVariable Long gameId,
            @RequestParam GameVisibility visibility,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            gameService.updateGameVisibility(gameId, authentication.getName(), visibility);
            redirectAttributes.addFlashAttribute("successMessage", "Visibility updated.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/games";
    }

    @PostMapping("/{gameId}/duplicate")
    public String duplicateGame(
            @PathVariable Long gameId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            gameService.duplicateCustomGame(gameId, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Game duplicated.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/games";
    }

    @PostMapping("/{gameId}/delete")
    public String deleteGame(
            @PathVariable Long gameId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            gameService.deleteCustomGame(gameId, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Game, assignments, attempts, answers and questions deleted.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/games";
    }

    @PostMapping("/{gameId}/questions/add")
    public String addQuestion(
            @PathVariable Long gameId,
            @RequestParam(required = false) String questionText,
            @RequestParam(required = false) String optionA,
            @RequestParam(required = false) String optionB,
            @RequestParam(required = false) String optionC,
            @RequestParam(required = false) String optionD,
            @RequestParam(required = false) String correctAnswer,
            @RequestParam(required = false) String leftItem,
            @RequestParam(required = false) String rightItem,
            @RequestParam(required = false) String leftImageUrl,
            @RequestParam(required = false) PuzzleTechnique puzzleTechnique,
            @RequestParam(required = false) String suppliedItems,
            @RequestParam(required = false) String expectedSequence,
            @RequestParam(required = false) String pattern,
            @RequestParam(required = false) String missingValues,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            if (puzzleTechnique != null) {
                questionService.addPuzzle(gameId, authentication.getName(), questionText,
                        puzzleTechnique, suppliedItems, expectedSequence, pattern, missingValues);
            } else {
                questionService.addQuestion(gameId, authentication.getName(),
                        question(questionText, optionA, optionB, optionC, optionD,
                                correctAnswer, leftItem, rightItem, leftImageUrl), null);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Question added.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/games/" + gameId + "/manage";
    }

    @PostMapping("/{gameId}/questions/matching/add")
    public String addMatchingPairs(
            @PathVariable Long gameId,
            @RequestParam(required = false) String questionText,
            @RequestParam List<String> leftItems,
            @RequestParam List<String> rightItems,
            @RequestParam(required = false) List<String> leftImageUrls,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            List<GameQuestion> savedPairs = questionService.addMatchingPairs(
                    gameId, authentication.getName(), questionText,
                    leftItems, rightItems, leftImageUrls);
            redirectAttributes.addFlashAttribute("successMessage",
                    savedPairs.size() + " matching pair(s) added.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/games/" + gameId + "/manage";
    }

    @PostMapping("/{gameId}/questions/{questionId}/update")
    public String updateQuestion(
            @PathVariable Long gameId,
            @PathVariable Long questionId,
            @RequestParam(required = false) String questionText,
            @RequestParam(required = false) String optionA,
            @RequestParam(required = false) String optionB,
            @RequestParam(required = false) String optionC,
            @RequestParam(required = false) String optionD,
            @RequestParam(required = false) String correctAnswer,
            @RequestParam(required = false) String leftItem,
            @RequestParam(required = false) String rightItem,
            @RequestParam(required = false) String leftImageUrl,
            @RequestParam(required = false) PuzzleTechnique puzzleTechnique,
            @RequestParam(required = false) String suppliedItems,
            @RequestParam(required = false) String expectedSequence,
            @RequestParam(required = false) String pattern,
            @RequestParam(required = false) String missingValues,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            if (puzzleTechnique != null) {
                questionService.updatePuzzle(gameId, questionId, authentication.getName(),
                        questionText, puzzleTechnique, suppliedItems, expectedSequence,
                        pattern, missingValues);
            } else {
                questionService.updateQuestion(gameId, questionId,
                        authentication.getName(), question(questionText,
                                optionA, optionB, optionC, optionD, correctAnswer,
                                leftItem, rightItem, leftImageUrl), null);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Question updated.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/games/" + gameId + "/manage";
    }

    @PostMapping("/{gameId}/questions/{questionId}/delete")
    public String deleteQuestion(
            @PathVariable Long gameId,
            @PathVariable Long questionId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            questionService.deleteQuestion(gameId, questionId, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Question deleted.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/teacher/games/" + gameId + "/manage";
    }

    private GameQuestion question(
            String text, String optionA, String optionB, String optionC,
            String optionD, String correctAnswer, String leftItem,
            String rightItem, String leftImageUrl) {
        GameQuestion question = new GameQuestion();
        question.setQuestionText(text);
        question.setOptionA(optionA);
        question.setOptionB(optionB);
        question.setOptionC(optionC);
        question.setOptionD(optionD);
        question.setCorrectAnswer(correctAnswer);
        question.setLeftItem(leftItem);
        question.setRightItem(rightItem);
        question.setLeftImageUrl(leftImageUrl);
        return question;
    }

    private void addOptions(Model model) {
        model.addAttribute("subjects", SUBJECTS);
        model.addAttribute("gameTypes", GameType.values());
        model.addAttribute("visibilities", GameVisibility.values());
        model.addAttribute("puzzleTemplates",
                List.of(PuzzleTemplate.WORD, PuzzleTemplate.NUMBER));
        model.addAttribute("matchingFormats", MatchingFormat.values());
    }

    private List<PuzzleTechnique> puzzleTechniquesFor(Game game) {
        if (game.getPuzzleTemplate() == PuzzleTemplate.WORD) {
            return List.of(PuzzleTechnique.ALPHABET_SEQUENCE,
                    PuzzleTechnique.MISSING_ALPHABET);
        }
        if (game.getPuzzleTemplate() != PuzzleTemplate.NUMBER) {
            return List.of();
        }
        return List.of(PuzzleTechnique.NUMBER_SEQUENCE, PuzzleTechnique.MISSING_NUMBER);
    }

    public record GameView(
            Long id, String title, String description, String subject,
            Integer grade, GameType gameType, GameSource source,
            GameVisibility visibility, PuzzleTemplate puzzleTemplate,
            MatchingFormat matchingFormat) {
        static GameView from(Game game) {
            return new GameView(game.getId(), game.getTitle(), game.getDescription(),
                    game.getSubject(), game.getGrade(), game.getGameType(),
                    game.getSource(), game.getVisibility(), game.getPuzzleTemplate(),
                    game.getMatchingFormat());
        }
    }

    public record QuestionView(
            Long id, Integer questionOrder, String questionText,
            String optionA, String optionB, String optionC, String optionD,
            String correctAnswer, String leftItem, String rightItem,
            String leftImageUrl, String rightImageUrl, String puzzleData,
            PuzzleTechnique puzzleTechnique, String puzzleTokens,
            String puzzleAnswerDisplay) {
        static QuestionView from(GameQuestion question) {
            PuzzleTechnique technique = PuzzleConfiguration.techniqueFrom(question.getPuzzleData());
            String displayAnswer = question.getCorrectAnswer();
            if (technique == PuzzleTechnique.ALPHABET_SEQUENCE && displayAnswer != null) {
                displayAnswer = displayAnswer.replace(",", "");
            }
            return new QuestionView(question.getId(), question.getQuestionOrder(),
                    question.getQuestionText(), question.getOptionA(), question.getOptionB(),
                    question.getOptionC(), question.getOptionD(), question.getCorrectAnswer(),
                    question.getLeftItem(), question.getRightItem(), question.getLeftImageUrl(),
                    question.getRightImageUrl(), question.getPuzzleData(),
                    technique,
                    PuzzleConfiguration.parseDefinition(question.getPuzzleData()) == null
                            ? "" : String.join(",", PuzzleConfiguration.parseDefinition(question.getPuzzleData()).tokens()),
                    displayAnswer);
        }
    }
}
