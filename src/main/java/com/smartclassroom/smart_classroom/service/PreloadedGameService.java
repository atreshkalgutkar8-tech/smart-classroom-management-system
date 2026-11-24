package com.smartclassroom.smart_classroom.service;

import com.smartclassroom.smart_classroom.entity.Game;
import com.smartclassroom.smart_classroom.entity.GameQuestion;
import com.smartclassroom.smart_classroom.entity.GameSource;
import com.smartclassroom.smart_classroom.entity.GameType;
import com.smartclassroom.smart_classroom.entity.GameVisibility;
import com.smartclassroom.smart_classroom.entity.MatchingFormat;
import com.smartclassroom.smart_classroom.entity.PuzzleTemplate;
import com.smartclassroom.smart_classroom.entity.PuzzleTechnique;
import com.smartclassroom.smart_classroom.repository.GameQuestionRepository;
import com.smartclassroom.smart_classroom.repository.GameAttemptAnswerRepository;
import com.smartclassroom.smart_classroom.repository.GameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PreloadedGameService {

    private static final List<String> SUBJECTS =
            List.of("Mathematics", "Kannada", "English", "EVS");

    private final GameRepository gameRepository;
    private final GameQuestionRepository questionRepository;
    private final GameAttemptAnswerRepository answerRepository;

    public PreloadedGameService(
            GameRepository gameRepository,
            GameQuestionRepository questionRepository,
            GameAttemptAnswerRepository answerRepository) {
        this.gameRepository = gameRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    @Transactional
    public void seedIfMissing() {
        for (int grade = 1; grade <= 5; grade++) {
            for (int subjectIndex = 0; subjectIndex < SUBJECTS.size(); subjectIndex++) {
                String subject = SUBJECTS.get(subjectIndex);
                addIfMissing(quiz(grade, subject));
                addIfMissing(matching(grade, subject));
                PuzzleTemplate template = (grade + subjectIndex) % 2 == 0
                        ? PuzzleTemplate.WORD : PuzzleTemplate.NUMBER;
                addIfMissing(puzzle(grade, subject, template));
            }
        }
    }

    private void addIfMissing(Seed seed) {
        var existing = gameRepository.findFirstBySourceAndTitleAndGradeAndSubjectAndGameType(
                GameSource.PRELOADED, seed.game().getTitle(), seed.game().getGrade(),
                seed.game().getSubject(), seed.game().getGameType());
        if (existing.isPresent()) {
            return;
        }

        Game game = gameRepository.save(seed.game());
        GameQuestion question = seed.question();
        question.setGame(game);
        questionRepository.save(question);
    }

    private Seed quiz(int grade, String subject) {
        Game game = baseGame(grade, subject, GameType.QUIZ,
                "Grade " + grade + " " + subject + " Quiz");
        GameQuestion question = question("Answer the question.");
        String answer;
        List<String> options;

        switch (subject) {
            case "Mathematics" -> {
                int result = grade + 4;
                question.setQuestionText("What is " + (grade + 2) + " + 2?");
                answer = Integer.toString(result);
                options = List.of(answer, Integer.toString(result + 1),
                        Integer.toString(result - 1), Integer.toString(result + 2));
            }
            case "Kannada" -> {
                question.setQuestionText("ಕನ್ನಡ ವರ್ಣಮಾಲೆಯಲ್ಲಿ ಮೊದಲ ಸ್ವರ ಯಾವುದು?");
                answer = "ಅ";
                options = List.of("ಅ", "ಕ", "ಮ", "ಇ");
            }
            case "English" -> {
                question.setQuestionText("Which word names a person, place, or thing?");
                answer = "teacher";
                options = List.of("quickly", "teacher", "blue", "under");
            }
            default -> {
                String[] prompts = {
                        "Which thing do plants need to grow?",
                        "Which sense organ helps us see?",
                        "Which plant part absorbs water?",
                        "Which is a renewable source of energy?",
                        "Which planet is our home?"
                };
                String[] answers = {"water", "eyes", "roots", "sunlight", "Earth"};
                question.setQuestionText(prompts[grade - 1]);
                answer = answers[grade - 1];
                options = List.of(answer, "plastic", "stone", "smoke");
            }
        }

        question.setOptionA(options.get(0));
        question.setOptionB(options.get(1));
        question.setOptionC(options.get(2));
        question.setOptionD(options.get(3));
        question.setCorrectAnswer(answer);
        question.setQuestionOrder(1);
        return new Seed(game, question);
    }

    private Seed matching(int grade, String subject) {
        Game game = baseGame(grade, subject, GameType.MATCHING,
                "Grade " + grade + " " + subject + " Matching");
        game.setMatchingFormat(MatchingFormat.TEXT_TO_TEXT);

        GameQuestion question = question("Match each item with its related meaning.");
        switch (subject) {
            case "Mathematics" -> {
                question.setLeftItem(grade + " × 2");
                question.setRightItem(Integer.toString(grade * 2));
            }
            case "Kannada" -> {
                question.setLeftItem("ನಮಸ್ಕಾರ");
                question.setRightItem("Greeting");
            }
            case "English" -> {
                question.setLeftItem("sun");
                question.setRightItem("a star");
            }
            default -> {
                question.setLeftItem("Sun");
                question.setRightItem("Light and heat");
            }
        }
        question.setQuestionOrder(1);
        return new Seed(game, question);
    }

    private Seed puzzle(int grade, String subject, PuzzleTemplate template) {
        Game game = baseGame(grade, subject, GameType.PUZZLE,
                "Grade " + grade + " " + subject + " "
                        + (template == PuzzleTemplate.WORD
                            ? "Alphabet Sequence Puzzle" : "Number Sequence Puzzle"));
        game.setPuzzleTemplate(template);
        String prompt;
        PuzzleConfiguration.PuzzleContent content;
        if (template == PuzzleTemplate.WORD) {
            String[][] words = {{"C,A,T", "CAT"}, {"D,O,G", "DOG"},
                    {"S,U,N", "SUN"}, {"B,O,O,K", "BOOK"}, {"P,L,A,N,E,T", "PLANET"}};
            String[] word = words[grade - 1];
            if ((grade + subject.length()) % 2 == 0) {
                String[][] patterns = {{"C,A,__", "T"}, {"D,__,G", "O"},
                        {"S,__,N", "U"}, {"B,O,__,K", "O"}, {"P,L,A,N,__,T", "E"}};
                String[] pattern = patterns[grade - 1];
                prompt = "Fill in the missing letter.";
                content = PuzzleConfiguration.build(template, grade,
                        PuzzleTechnique.MISSING_ALPHABET, null, null, pattern[0], pattern[1]);
            } else {
                prompt = "Arrange the letters to make a word.";
                content = PuzzleConfiguration.build(template, grade,
                        PuzzleTechnique.ALPHABET_SEQUENCE, word[0], word[1], null, null);
            }
        } else if (grade % 2 == 1) {
            String[][] sequences = {{"3,1,2", "1,2,3"}, {"5,2,4,1,3", "1,2,3,4,5"},
                    {"4,2,5,1,3", "1,2,3,4,5"}, {"8,6,9,7,5", "5,6,7,8,9"},
                    {"12,10,14,11,13", "10,11,12,13,14"}};
            String[] sequence = sequences[grade - 1];
            prompt = "Arrange the numbers from smallest to largest.";
            content = PuzzleConfiguration.build(template, grade,
                    PuzzleTechnique.NUMBER_SEQUENCE, sequence[0], sequence[1], null, null);
        } else {
            String[][] patterns = {{"2,__,4", "3"}, {"2,4,__,8", "6"},
                    {"10,__,30", "20"}, {"5,10,__,20", "15"}, {"10,20,__,40", "30"}};
            String[] pattern = patterns[grade - 1];
            prompt = "Fill in the missing number.";
            content = PuzzleConfiguration.build(template, grade,
                    PuzzleTechnique.MISSING_NUMBER, null, null, pattern[0], pattern[1]);
        }
        GameQuestion question = question(prompt);
        question.setCorrectAnswer(content.correctAnswer());
        question.setPuzzleData(content.puzzleData());
        question.setQuestionOrder(1);
        return new Seed(game, question);
    }
    private Game baseGame(int grade, String subject, GameType type, String title) {
        Game game = new Game();
        game.setTitle(title);
        game.setDescription("Grade " + grade + " " + subject
                + " practice activity.");
        game.setGameType(type);
        game.setSubject(subject);
        game.setGrade(grade);
        game.setSource(GameSource.PRELOADED);
        game.setCreatedBy(null);
        game.setVisibility(GameVisibility.AVAILABLE_TO_STUDENTS);
        return game;
    }

    private GameQuestion question(String text) {
        GameQuestion question = new GameQuestion();
        question.setQuestionText(text);
        return question;
    }

    private record Seed(Game game, GameQuestion question) {
    }
}
