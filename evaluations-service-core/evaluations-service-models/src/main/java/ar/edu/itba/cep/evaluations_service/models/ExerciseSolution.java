package ar.edu.itba.cep.evaluations_service.models;

import org.springframework.util.Assert;

import java.util.Objects;

/**
 * Represents a solution of an exercise.
 */
public class ExerciseSolution {

    /**
     * The exercise solution's id.
     */
    private final long id;
    /**
     * The {@link Exercise} to which it belongs to.
     */
    private final Exercise exercise;
    /**
     * The answer to the question of the {@link Exercise} (i.e the code written by the student).
     */
    private final String answer;


    /**
     * Default constructor.
     */
    /* package */ ExerciseSolution() {
        // Initialize final fields with default values.
        this.id = 0;
        this.exercise = null;
        this.answer = null;
    }

    /**
     * Constructor.
     *
     * @param exercise The {@link Exercise} to which it belongs to.
     * @param answer   The answer to the question of the {@link Exercise} (i.e the code written by the student).
     * @throws IllegalArgumentException If any argument is not valid.
     */
    public ExerciseSolution(final Exercise exercise, final String answer) throws IllegalArgumentException {
        assertExercise(exercise);
        assertAnswer(answer);
        this.id = 0;
        this.exercise = exercise;
        this.answer = answer;
    }


    /**
     * @return The exercise solution's id.
     */
    public long getId() {
        return id;
    }

    /**
     * @return The {@link Exercise} to which it belongs to.
     */
    public Exercise getExercise() {
        return exercise;
    }

    /**
     * @return The answer to the question of the {@link Exercise} (i.e the code written by the student).
     */
    public String getAnswer() {
        return answer;
    }


    // ================================
    // equals, hashcode and toString
    // ================================

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExerciseSolution)) {
            return false;
        }
        final var that = (ExerciseSolution) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ExerciseSolution [" +
                "ID: " + id + ", " +
                "Exercise: " + exercise + ", " +
                "Answer: '" + answer + "'" +
                "]";
    }


    // ================================
    // Assertions
    // ================================

    /**
     * Asserts that the given {@code exercise} is valid.
     *
     * @param exercise The {@link Exercise} to be checked.
     * @throws IllegalArgumentException If the exercise is not valid.
     */
    private static void assertExercise(final Exercise exercise) throws IllegalArgumentException {
        Assert.notNull(exercise, "The exercise is missing");
    }

    /**
     * Asserts that the given {@code answer} is valid.
     *
     * @param answer The answer to be checked.
     * @throws IllegalArgumentException If the answer is not valid.
     */
    private static void assertAnswer(final String answer) throws IllegalArgumentException {
        Assert.notNull(answer, "The answer is missing");
        Assert.isTrue(answer.length() >= ValidationConstants.ANSWER_MIN_LENGTH,
                "The answer is too short");
    }
}
