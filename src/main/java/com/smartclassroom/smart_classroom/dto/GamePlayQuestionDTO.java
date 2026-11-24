
package com.smartclassroom.smart_classroom.dto;

import com.smartclassroom.smart_classroom.entity.GameQuestion;
import com.smartclassroom.smart_classroom.entity.PuzzleTechnique;
import com.smartclassroom.smart_classroom.service.PuzzleConfiguration;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class GamePlayQuestionDTO {

    private Long id;
    private String questionText;

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    private String leftItem;
    private String leftImageUrl;
    private String savedAnswer;
    private boolean savedCorrect;
    private boolean savedEvaluated;
    private List<String> matchingOptions = List.of();

    private Integer questionOrder;
    private PuzzleTechnique puzzleTechnique;
    private List<PuzzleTile> puzzleTiles = List.of();
    private List<String> puzzlePattern = List.of();

    public GamePlayQuestionDTO(GameQuestion question, String savedAnswer) {
        this(question, savedAnswer, false, List.of());
    }

    public GamePlayQuestionDTO(
            GameQuestion question,
            String savedAnswer,
            boolean savedCorrect,
            List<String> matchingOptions) {
        this(question, savedAnswer, savedCorrect, false, matchingOptions);
    }

    public GamePlayQuestionDTO(
            GameQuestion question,
            String savedAnswer,
            boolean savedCorrect,
            boolean savedEvaluated,
            List<String> matchingOptions) {

        this.id = question.getId();
        this.questionText = question.getQuestionText();

        this.optionA = question.getOptionA();
        this.optionB = question.getOptionB();
        this.optionC = question.getOptionC();
        this.optionD = question.getOptionD();

        this.leftItem = question.getLeftItem();
        this.leftImageUrl = question.getLeftImageUrl();
        this.savedAnswer = savedAnswer;
        this.savedCorrect = savedCorrect;
        this.savedEvaluated = savedEvaluated;
        this.matchingOptions = List.copyOf(matchingOptions);

        this.questionOrder = question.getQuestionOrder();
        PuzzleConfiguration.PuzzleDefinition definition = PuzzleConfiguration.parseDefinition(question.getPuzzleData());
        this.puzzleTechnique = definition == null ? null : definition.technique();
        if (definition != null) {
            this.puzzlePattern = definition.tokens();
            if (puzzleTechnique == PuzzleTechnique.NUMBER_SEQUENCE
                    || puzzleTechnique == PuzzleTechnique.ALPHABET_SEQUENCE) {
                List<PuzzleTile> tiles = new ArrayList<>();
                for (int index = 0; index < definition.tokens().size(); index++) {
                    tiles.add(new PuzzleTile(index, definition.tokens().get(index)));
                }
                Collections.shuffle(tiles);
                this.puzzleTiles = List.copyOf(tiles);
            }
        }
    }

    public Long getId() {
        return id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getOptionA() {
        return optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public String getLeftItem() {
        return leftItem;
    }

    public String getLeftImageUrl() {
        return leftImageUrl;
    }

    public String getSavedAnswer() {
        return savedAnswer;
    }

    public boolean isSavedCorrect() {
        return savedCorrect;
    }

    public boolean isSavedEvaluated() {
        return savedEvaluated;
    }

    public List<String> getMatchingOptions() {
        return matchingOptions;
    }

    public Integer getQuestionOrder() {
        return questionOrder;
    }

    public PuzzleTechnique getPuzzleTechnique() {
        return puzzleTechnique;
    }

    public List<PuzzleTile> getPuzzleTiles() { return puzzleTiles; }
    public List<String> getPuzzlePattern() { return puzzlePattern; }

    public record PuzzleTile(int index, String value) { }

}
