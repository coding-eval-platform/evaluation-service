package ar.edu.itba.cep.evaluations_service.rest.controller.dtos;

import ar.edu.itba.cep.evaluations_service.models.Exercise;
import ar.edu.itba.cep.evaluations_service.models.ValidationConstants;
import com.bellotapps.webapps_commons.errors.ConstraintViolationError.ErrorCausePayload.IllegalValue;
import com.bellotapps.webapps_commons.errors.ConstraintViolationError.ErrorCausePayload.MissingValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Data transfer object for receiving {@link Exercise}s data from an API consumer.
 */
public class ExerciseUploadDto {

    /**
     * The question for the exercise.
     */
    @NotNull(message = "The question is missing.", payload = MissingValue.class,
            groups = {
                    Create.class,
                    ChangeQuestion.class,
            }
    )
    @Size(message = "Question too short", payload = IllegalValue.class,
            min = ValidationConstants.QUESTION_MIN_LENGTH,
            groups = {
                    Create.class,
                    ChangeQuestion.class,
            }
    )
    private final String question;


    /**
     * Constructor.
     */
    @JsonCreator
    public ExerciseUploadDto(
            @JsonProperty(value = "question", access = JsonProperty.Access.WRITE_ONLY) final String question) {
        this.question = question;
    }


    /**
     * @return The question for the exercise.
     */
    public String getQuestion() {
        return question;
    }


    // ================================================================================================================
    // Validation groups
    // ================================================================================================================

    /**
     * Validation group for the create operation.
     */
    public interface Create {
    }

    /**
     * Validation group for the change question operation.
     */
    public interface ChangeQuestion {
    }
}