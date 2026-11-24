package com.smartclassroom.smart_classroom.service;

import com.smartclassroom.smart_classroom.entity.GameQuestion;
import com.smartclassroom.smart_classroom.entity.PuzzleTemplate;
import com.smartclassroom.smart_classroom.entity.PuzzleTechnique;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Encodes supported puzzle inputs in the existing puzzleData column. */
public final class PuzzleConfiguration {

    private static final String PREFIX = "PUZZLE_V2|";
    private static final String BLANK = "__";

    private PuzzleConfiguration() {
    }

    public static PuzzleContent build(
            PuzzleTemplate template,
            int grade,
            PuzzleTechnique technique,
            String suppliedItems,
            String expectedSequence,
            String pattern,
            String missingValues) {
        validateGrade(grade);
        if (technique == null || !supportsTemplate(template, technique)) {
            throw new IllegalArgumentException(
                    "Choose one of the puzzle techniques supported by this game.");
        }

        List<String> tokens;
        List<String> answers;
        if (technique == PuzzleTechnique.NUMBER_SEQUENCE) {
            tokens = parseNumbers(suppliedItems, grade, grade + 4);
            answers = parseNumbers(expectedSequence, grade, grade + 4);
            if (tokens.size() < 3 || tokens.size() != answers.size()
                    || !frequencies(tokens).equals(frequencies(answers))) {
                throw new IllegalArgumentException(
                        "The shuffled numbers and correct sequence must contain the same values.");
            }
        } else if (technique == PuzzleTechnique.ALPHABET_SEQUENCE) {
            tokens = parseLettersCsv(suppliedItems, grade + 4);
            answers = parseLetters(expectedSequence, grade + 4);
            if (tokens.size() < 3 || tokens.size() != answers.size()
                    || !frequencies(tokens).equals(frequencies(answers))) {
                throw new IllegalArgumentException(
                        "The supplied letters and correct sequence must contain the same letters.");
            }
        } else {
            tokens = parsePattern(pattern, technique, grade);
            int blanks = (int) tokens.stream().filter(BLANK::equals).count();
            answers = technique == PuzzleTechnique.MISSING_NUMBER
                    ? parseNumbers(missingValues, grade, blanks)
                    : parseLettersCsv(missingValues, blanks);
            if (blanks == 0 || answers.size() != blanks) {
                throw new IllegalArgumentException(
                        "Provide exactly one expected value for every blank.");
            }
        }

        String correctAnswer = String.join(",", answers);
        if (correctAnswer.length() > 500) {
            throw new IllegalArgumentException("Puzzle answer is too long.");
        }
        return new PuzzleContent(encodeDefinition(technique, grade, tokens), correctAnswer,
                technique, tokens, answers);
    }

    private static List<String> parsePattern(
            String raw, PuzzleTechnique technique, int grade) {
        List<String> values = parseCsv(raw, "Enter a sequence with __ for each missing value.");
        if (values.size() < 3 || values.size() > grade + 4) {
            throw new IllegalArgumentException(
                    "The sequence length must be suitable for the selected grade.");
        }
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            if (BLANK.equals(value) || "_".equals(value)) {
                normalized.add(BLANK);
            } else if (technique == PuzzleTechnique.MISSING_NUMBER) {
                normalized.add(normalizeNumber(value, grade));
            } else {
                normalized.add(normalizeLetter(value));
            }
        }
        return List.copyOf(normalized);
    }

    private static List<String> parseNumbers(String raw, int grade, int maxCount) {
        List<String> values = parseCsv(raw, "Enter comma-separated whole numbers.");
        if (values.isEmpty() || values.size() > maxCount) {
            throw new IllegalArgumentException(
                    "The number sequence length is outside this grade's range.");
        }
        return values.stream().map(value -> normalizeNumber(value, grade)).toList();
    }

    private static String normalizeNumber(String value, int grade) {
        try {
            BigInteger number = new BigInteger(value.trim());
            int limit = numberLimit(grade);
            if (number.signum() < 0 || number.compareTo(BigInteger.valueOf(limit)) > 0) {
                throw new IllegalArgumentException(
                        "Numbers must fit the selected grade's range (0–" + limit + ").");
            }
            return number.toString();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Use whole numbers in puzzle sequences.");
        }
    }

    private static List<String> parseLettersCsv(String raw, int maxCount) {
        List<String> values = parseCsv(raw, "Enter comma-separated letters.");
        if (values.isEmpty() || values.size() > maxCount) {
            throw new IllegalArgumentException(
                    "The letter sequence length is outside this grade's range.");
        }
        return values.stream().map(PuzzleConfiguration::normalizeLetter).toList();
    }

    private static List<String> parseLetters(String raw, int maxCount) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Enter the correct letter sequence.");
        }
        String compact = raw.trim().replaceAll("\\s+", "");
        List<String> letters = compact.codePoints()
                .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                .map(PuzzleConfiguration::normalizeLetter)
                .toList();
        if (letters.isEmpty() || letters.size() > maxCount) {
            throw new IllegalArgumentException(
                    "The answer length is outside this grade's range.");
        }
        return letters;
    }

    private static String normalizeLetter(String value) {
        String letter = value == null ? "" : value.trim();
        if (letter.codePointCount(0, letter.length()) != 1
                || !Character.isLetter(letter.codePointAt(0))) {
            throw new IllegalArgumentException(
                    "Each alphabet item must be exactly one letter.");
        }
        String normalized = letter.toUpperCase(Locale.ROOT);
        if (normalized.codePointCount(0, normalized.length()) != 1) {
            throw new IllegalArgumentException("Use letters with a single uppercase form.");
        }
        return normalized;
    }

    private static List<String> parseCsv(String raw, String message) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        String[] parts = raw.split(",", -1);
        List<String> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            String value = part.trim();
            if (value.isEmpty()) {
                throw new IllegalArgumentException(message);
            }
            result.add(value);
        }
        return result;
    }

    private static <T> Map<T, Integer> frequencies(List<T> values) {
        Map<T, Integer> counts = new HashMap<>();
        values.forEach(value -> counts.merge(value, 1, Integer::sum));
        return counts;
    }

    private static int numberLimit(int grade) {
        return switch (grade) {
            case 1 -> 20;
            case 2 -> 100;
            case 3 -> 1_000;
            case 4 -> 10_000;
            default -> 100_000;
        };
    }

    private static void validateGrade(int grade) {
        if (grade < 1 || grade > 5) {
            throw new IllegalArgumentException("Puzzle grade must be between 1 and 5.");
        }
    }

    private static boolean supportsTemplate(
            PuzzleTemplate template, PuzzleTechnique technique) {
        return (template == PuzzleTemplate.NUMBER
                && (technique == PuzzleTechnique.NUMBER_SEQUENCE
                    || technique == PuzzleTechnique.MISSING_NUMBER))
                || (template == PuzzleTemplate.WORD
                && (technique == PuzzleTechnique.ALPHABET_SEQUENCE
                    || technique == PuzzleTechnique.MISSING_ALPHABET));
    }

    public static String encodeDefinition(
            PuzzleTechnique technique, int grade, List<String> tokens) {
        String encodedTokens = tokens.stream()
                .map(token -> Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(token.getBytes(StandardCharsets.UTF_8)))
                .collect(Collectors.joining(","));
        return PREFIX + technique.name() + "|" + grade + "|" + encodedTokens;
    }

    public static PuzzleDefinition parseDefinition(String data) {
        if (data == null || !data.startsWith(PREFIX)) {
            return null;
        }
        String[] fields = data.split("\\|", 4);
        if (fields.length != 4) {
            return null;
        }
        try {
            PuzzleTechnique technique = PuzzleTechnique.valueOf(fields[1]);
            int grade = Integer.parseInt(fields[2]);
            validateGrade(grade);
            List<String> tokens = new ArrayList<>();
            if (!fields[3].isEmpty()) {
                for (String token : fields[3].split(",", -1)) {
                    tokens.add(new String(Base64.getUrlDecoder().decode(token),
                            StandardCharsets.UTF_8));
                }
            }
            if (tokens.isEmpty() || tokens.stream().anyMatch(String::isEmpty)) {
                return null;
            }
            return new PuzzleDefinition(technique, grade, List.copyOf(tokens));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Legacy JSON configs return null unless they use one of the supported techniques. */
    public static PuzzleTechnique techniqueFrom(String puzzleData) {
        PuzzleDefinition definition = parseDefinition(puzzleData);
        if (definition != null) {
            return definition.technique();
        }
        if (puzzleData == null) {
            return null;
        }
        String marker = "\"technique\":\"";
        int start = puzzleData.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = puzzleData.indexOf('"', start);
        if (end < 0) {
            return null;
        }
        try {
            return PuzzleTechnique.valueOf(puzzleData.substring(start, end));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static List<String> expectedTokens(GameQuestion question) {
        if (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
            return List.of();
        }
        return List.of(question.getCorrectAnswer().split(",", -1));
    }

    /** Returns normalized draft data after checking every supplied item/position. */
    public static String validateSubmission(
            GameQuestion question, String submitted, boolean allowIncomplete) {
        PuzzleDefinition definition = parseDefinition(question.getPuzzleData());
        if (definition == null || !isSupportedQuestion(question)) {
            throw new IllegalArgumentException("This legacy puzzle is not supported for play.");
        }
        PuzzleTechnique technique = definition.technique();
        if (technique == PuzzleTechnique.NUMBER_SEQUENCE
                || technique == PuzzleTechnique.ALPHABET_SEQUENCE) {
            List<String> indices = parseDraftIndexes(submitted, definition.tokens().size(), allowIncomplete);
            if (!allowIncomplete && indices.size() != definition.tokens().size()) {
                throw new IllegalArgumentException("Place every supplied item before submitting.");
            }
            return String.join(",", indices);
        }

        int blankCount = (int) definition.tokens().stream().filter(BLANK::equals).count();
        List<String> values = splitDraftValues(submitted, blankCount, allowIncomplete);
        if (!allowIncomplete && (values.size() != blankCount
                || values.stream().anyMatch(String::isBlank))) {
            throw new IllegalArgumentException("Fill every missing position before submitting.");
        }
        List<String> normalized = new ArrayList<>(values.size());
        for (String value : values) {
            if (value.isBlank()) {
                if (!allowIncomplete) {
                    throw new IllegalArgumentException("Fill every missing position before submitting.");
                }
                normalized.add("");
            } else if (technique == PuzzleTechnique.MISSING_NUMBER) {
                normalized.add(normalizeNumber(value, definition.grade()));
            } else {
                normalized.add(normalizeLetter(value));
            }
        }
        return String.join(",", normalized);
    }

    private static List<String> parseDraftIndexes(String submitted, int itemCount, boolean allowIncomplete) {
        if (submitted == null || submitted.isBlank()) {
            return List.of();
        }
        String[] values = submitted.split(",", -1);
        Set<Integer> seen = new HashSet<>();
        List<String> normalized = new ArrayList<>(values.length);
        for (String value : values) {
            if (value.isBlank() && allowIncomplete) {
                normalized.add("");
                continue;
            }
            try {
                int index = Integer.parseInt(value.trim());
                if (index < 0 || index >= itemCount || !seen.add(index)) {
                    throw new IllegalArgumentException(
                            "Use every supplied tile at most once.");
                }
                normalized.add(Integer.toString(index));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Puzzle item order is invalid.");
            }
        }
        return normalized;
    }

    private static List<String> splitDraftValues(
            String submitted, int blankCount, boolean allowIncomplete) {
        if (submitted == null || submitted.isEmpty()) {
            return allowIncomplete
                    ? new ArrayList<>(java.util.Collections.nCopies(blankCount, ""))
                    : List.of("");
        }
        List<String> values = new ArrayList<>(
                List.of(submitted.split(",", -1)));
        if (allowIncomplete && values.size() < blankCount) {
            while (values.size() < blankCount) values.add("");
        }
        if (values.size() != blankCount) {
            throw new IllegalArgumentException("The number of submitted values does not match the blanks.");
        }
        return values;
    }

    public static boolean isCorrect(GameQuestion question, String submitted) {
        String normalized = validateSubmission(question, submitted, false);
        PuzzleDefinition definition = parseDefinition(question.getPuzzleData());
        List<String> expected = expectedTokens(question);
        if (definition.technique() == PuzzleTechnique.NUMBER_SEQUENCE
                || definition.technique() == PuzzleTechnique.ALPHABET_SEQUENCE) {
            List<String> orderedItems = java.util.Arrays.stream(normalized.split(","))
                    .map(Integer::parseInt)
                    .map(definition.tokens()::get)
                    .toList();
            return orderedItems.equals(expected);
        }
        return List.of(normalized.split(",", -1)).equals(expected);
    }

    public static boolean isSupportedQuestion(GameQuestion question) {
        PuzzleDefinition definition = parseDefinition(question.getPuzzleData());
        if (definition == null) {
            return false;
        }
        return (question.getGame().getPuzzleTemplate() == PuzzleTemplate.NUMBER
                && (definition.technique() == PuzzleTechnique.NUMBER_SEQUENCE
                    || definition.technique() == PuzzleTechnique.MISSING_NUMBER))
                || (question.getGame().getPuzzleTemplate() == PuzzleTemplate.WORD
                && (definition.technique() == PuzzleTechnique.ALPHABET_SEQUENCE
                    || definition.technique() == PuzzleTechnique.MISSING_ALPHABET));
    }

    public static boolean isSupportedGame(List<GameQuestion> questions) {
        return questions != null && !questions.isEmpty()
                && questions.stream().allMatch(PuzzleConfiguration::isSupportedQuestion);
    }

    public static String defaultInstruction(PuzzleTechnique technique) {
        return switch (technique) {
            case NUMBER_SEQUENCE -> "Arrange the numbers in the correct order.";
            case ALPHABET_SEQUENCE -> "Arrange the letters in the correct order.";
            case MISSING_NUMBER -> "Fill each blank with the missing number.";
            case MISSING_ALPHABET -> "Fill each blank with the missing letter.";
        };
    }

    public static String formatTokens(List<String> values) {
        return String.join(", ", values);
    }

    public record PuzzleDefinition(
            PuzzleTechnique technique, int grade, List<String> tokens) {
    }

    public record PuzzleContent(
            String puzzleData, String correctAnswer,
            PuzzleTechnique technique, List<String> tokens,
            List<String> expectedTokens) {
    }
}
