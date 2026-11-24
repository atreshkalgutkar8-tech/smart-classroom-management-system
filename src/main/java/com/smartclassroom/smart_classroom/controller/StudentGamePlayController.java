
package com.smartclassroom.smart_classroom.controller;

import org.springframework.stereotype.Controller;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartclassroom.smart_classroom.entity.GameAttempt;
import com.smartclassroom.smart_classroom.entity.GameType;
import com.smartclassroom.smart_classroom.service.GameAnswerService;
import com.smartclassroom.smart_classroom.service.GameAttemptService;
import com.smartclassroom.smart_classroom.service.GamePlayService;

import java.util.List;

@Controller
@RequestMapping("/student/games")
public class StudentGamePlayController {

    private final GameAttemptService gameAttemptService;
    private final GamePlayService gamePlayService;
    private final GameAnswerService gameAnswerService;

    public StudentGamePlayController(
            GameAttemptService gameAttemptService,
            GamePlayService gamePlayService,
            GameAnswerService gameAnswerService) {

        this.gameAttemptService = gameAttemptService;
        this.gamePlayService = gamePlayService;
        this.gameAnswerService = gameAnswerService;
    }

    // Open game or resume unfinished attempt
    @GetMapping("/{assignmentId}/play")
    public String playGame(
            @PathVariable Long assignmentId,
            @RequestParam(defaultValue = "0") int questionIndex,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            String studentEmail = authentication.getName();

            GameAttempt attempt =
                    gameAttemptService.startOrResumeAttempt(
                            assignmentId, studentEmail);

            var questions =
                    gamePlayService.getQuestionsForAttempt(
                            attempt.getId(), studentEmail);
            int selectedQuestionIndex = questions.isEmpty()
                    ? 0 : Math.max(0, Math.min(questionIndex, questions.size() - 1));

            model.addAttribute("attempt", attempt);
            model.addAttribute("assignment", attempt.getAssignment());
            model.addAttribute("game", attempt.getAssignment().getGame());
            model.addAttribute("questions", questions);
            model.addAttribute("questionIndex", selectedQuestionIndex);
            model.addAttribute("currentQuestion", questions.isEmpty()
                    ? null : questions.get(selectedQuestionIndex));
            model.addAttribute("matchingOptions", questions.isEmpty()
                    ? java.util.List.of() : questions.get(0).getMatchingOptions());
            model.addAttribute("matchingQuestionsAnswered",
                    attempt.getAssignment().getGame().getGameType() == GameType.MATCHING
                            && !questions.isEmpty()
                            && questions.stream().allMatch(question ->
                                    question.isSavedEvaluated()
                                            && question.getSavedAnswer() != null
                                            && !question.getSavedAnswer().isBlank()));
            model.addAttribute("puzzleQuestionsAnswered",
                    attempt.getAssignment().getGame().getGameType() == GameType.PUZZLE
                            && !questions.isEmpty()
                            && questions.stream().allMatch(question ->
                                    question.isSavedEvaluated()
                                            && question.getSavedAnswer() != null
                                            && !question.getSavedAnswer().isBlank()));

            return "student-game-play";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());

            return "redirect:/student/games";
        }
    }

    // Save or update an answer
    @PostMapping("/{assignmentId}/attempts/{attemptId}/answer")
    public String saveAnswer(
            @PathVariable Long assignmentId,
            @PathVariable Long attemptId,
            @RequestParam Long questionId,
            @RequestParam String studentAnswer,
            @RequestParam(required = false) Integer nextQuestionIndex,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        boolean answerSaved = false;
        try {
            gameAnswerService.saveAnswer(
                    attemptId,
                    questionId,
                    studentAnswer,
                    authentication.getName()
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Your answer has been saved!");
            answerSaved = true;

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        String redirect = "redirect:/student/games/" + assignmentId + "/play";
        if (nextQuestionIndex != null) {
            int destination = answerSaved
                    ? nextQuestionIndex : Math.max(0, nextQuestionIndex - 1);
            redirect += "?questionIndex=" + destination;
        }
        return redirect;
    }

    @PostMapping("/{assignmentId}/attempts/{attemptId}/matching-progress")
    @ResponseBody
    public ResponseEntity<String> saveMatchingProgress(
            @PathVariable Long assignmentId,
            @PathVariable Long attemptId,
            @RequestParam("questionIds") List<Long> questionIds,
            @RequestParam("rightItems") List<String> rightItems,
            Authentication authentication) {
        try {
            gameAnswerService.saveMatchingDraft(
                    assignmentId, attemptId, questionIds, rightItems,
                    authentication.getName());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{assignmentId}/attempts/{attemptId}/matching-submit")
    public String submitMatchingAnswers(
            @PathVariable Long assignmentId,
            @PathVariable Long attemptId,
            @RequestParam("questionIds") List<Long> questionIds,
            @RequestParam("rightItems") List<String> rightItems,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            int score = gameAnswerService.submitMatchingBoard(
                    assignmentId, attemptId, questionIds, rightItems,
                    authentication.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Matching answers scored: " + score + ".");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/student/games/" + assignmentId + "/play";
    }

    // Complete the game attempt
    @PostMapping("/{assignmentId}/attempts/{attemptId}/complete")
    public String completeGame(
            @PathVariable Long assignmentId,
            @PathVariable Long attemptId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            gameAnswerService.completeAttempt(
                    attemptId,
                    authentication.getName()
            );

            return "redirect:/student/games/attempts/"
                    + attemptId + "/result";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());

            return "redirect:/student/games/"
                    + assignmentId + "/play";
        }
    }
}
