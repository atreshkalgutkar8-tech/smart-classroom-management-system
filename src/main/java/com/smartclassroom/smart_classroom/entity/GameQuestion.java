
package com.smartclassroom.smart_classroom.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "game_questions")
public class GameQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The game to which this question belongs
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    // Question or puzzle instruction
    @Column(nullable = false, length = 1000)
    private String questionText;

    // Quiz answer options
    @Column(length = 500)
    private String optionA;

    @Column(length = 500)
    private String optionB;

    @Column(length = 500)
    private String optionC;

    @Column(length = 500)
    private String optionD;

    // Correct answer for quiz
    @Column(length = 500)
    private String correctAnswer;

    // Matching game content
    @Column(length = 500)
    private String leftItem;

    @Column(length = 500)
    private String rightItem;

    // Image paths for matching or picture puzzles
    @Column(length = 1000)
    private String leftImageUrl;

    @Column(length = 1000)
    private String rightImageUrl;

    // Puzzle template content
    @Column(length = 2000)
    private String puzzleData;

    // Display order inside the game
    @Column(nullable = false)
    private Integer questionOrder;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getLeftItem() {
        return leftItem;
    }

    public void setLeftItem(String leftItem) {
        this.leftItem = leftItem;
    }

    public String getRightItem() {
        return rightItem;
    }

    public void setRightItem(String rightItem) {
        this.rightItem = rightItem;
    }

    public String getLeftImageUrl() {
        return leftImageUrl;
    }

    public void setLeftImageUrl(String leftImageUrl) {
        this.leftImageUrl = leftImageUrl;
    }

    public String getRightImageUrl() {
        return rightImageUrl;
    }

    public void setRightImageUrl(String rightImageUrl) {
        this.rightImageUrl = rightImageUrl;
    }

    public String getPuzzleData() {
        return puzzleData;
    }

    public void setPuzzleData(String puzzleData) {
        this.puzzleData = puzzleData;
    }

    public Integer getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(Integer questionOrder) {
        this.questionOrder = questionOrder;
    }
}
