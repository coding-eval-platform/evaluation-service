package ar.edu.itba.cep.evaluations_service.domain;

import ar.edu.itba.cep.evaluations_service.commands.executor_service.*;
import ar.edu.itba.cep.evaluations_service.models.*;
import ar.edu.itba.cep.evaluations_service.repositories.*;
import com.bellotapps.webapps_commons.persistence.repository_utils.paging_and_sorting.Page;
import com.bellotapps.webapps_commons.persistence.repository_utils.paging_and_sorting.PagingRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.internal.hamcrest.HamcrestArgumentMatcher;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

/**
 * Test class for {@link ExamManager}, containing tests for the happy paths
 * (i.e how the manager behaves when operating with valid values, entity states, etc.).
 */
@ExtendWith(MockitoExtension.class)
class ExamManagerHappyPathsTest extends AbstractExamManagerTest {

    /**
     * Constructor.
     *
     * @param examRepository             A mocked {@link ExamRepository} passed to super class.
     * @param exerciseRepository         A mocked {@link ExerciseRepository} passed to super class.
     * @param testCaseRepository         A mocked {@link TestCaseRepository} passed to super class.
     * @param exerciseSolutionRepository A mocked {@link ExerciseSolutionRepository} passed to super class.
     * @param exerciseSolResultRep       A mocked {@link ExerciseSolutionResultRepository} passed to super class.
     * @param executorServiceProxy       A mocked {@link ExecutorServiceCommandMessageProxy} passed to super class.
     */
    ExamManagerHappyPathsTest(
            @Mock(name = "examRep") final ExamRepository examRepository,
            @Mock(name = "exerciseRep") final ExerciseRepository exerciseRepository,
            @Mock(name = "testCaseRep") final TestCaseRepository testCaseRepository,
            @Mock(name = "exerciseSolutionRep") final ExerciseSolutionRepository exerciseSolutionRepository,
            @Mock(name = "exerciseSolutionResultRep") final ExerciseSolutionResultRepository exerciseSolResultRep,
            @Mock(name = "executorServiceProxy") final ExecutorServiceCommandMessageProxy executorServiceProxy) {
        super(examRepository,
                exerciseRepository,
                testCaseRepository,
                exerciseSolutionRepository,
                exerciseSolResultRep,
                executorServiceProxy);
    }


    // ================================================================================================================
    // Exams
    // ================================================================================================================

    /**
     * Tests that searching for an {@link Exam} that exists returns the expected {@link Exam}.
     *
     * @param exam A mocked {@link Exam} (which is returned by {@link ExamManager#getExam(long)}).
     */
    @Test
    void testSearchForExamThatExists(@Mock(name = "exam") final Exam exam) {
        final var examId = TestHelper.validExamId();
        when(exam.getId()).thenReturn(examId);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        final var examOptional = examManager.getExam(examId);
        Assertions.assertAll("Searching for an exam that exists is not working as expected",
                () -> Assertions.assertTrue(
                        examOptional.isPresent(),
                        "The returned Optional is empty"
                ),
                () -> Assertions.assertEquals(
                        examId,
                        examOptional.map(Exam::getId).get().longValue(),
                        "The returned Exam id's is not the same as the requested"
                )
        );
        verifyOnlyExamSearch(examId);
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }


    /**
     * Tests that an {@link Exam} is created (i.e is saved) when arguments are valid.
     */
    @Test
    void testExamIsCreatedUsingValidArguments() {
        final var description = TestHelper.validExamDescription();
        final var startingAt = TestHelper.validExamStartingMoment();
        final var duration = TestHelper.validExamDuration();
        when(examRepository.save(any(Exam.class))).then(invocation -> invocation.getArgument(0));
        final var exam = examManager.createExam(description, startingAt, duration);
        Assertions.assertAll("Exam properties are not the expected",
                () -> Assertions.assertEquals(
                        description,
                        exam.getDescription(),
                        "There is a mismatch in the description"
                ),
                () -> Assertions.assertEquals(
                        startingAt,
                        exam.getStartingAt(),
                        "There is a mismatch in the starting moment"
                ),
                () -> Assertions.assertEquals(
                        duration,
                        exam.getDuration(),
                        "There is a mismatch in the duration"
                )
        );
        verify(examRepository, only()).save(any(Exam.class));
        verifyZeroInteractions(exerciseRepository);
        verifyZeroInteractions(testCaseRepository);
        verifyZeroInteractions(exerciseSolutionRepository);
        verifyZeroInteractions(exerciseSolutionResultRepository);
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }


    /**
     * Tests that an {@link Exam} is updated (i.e is saved) when arguments are valid.
     *
     * @param exam A mocked {@link Exam} (the one being updated).
     */
    @Test
    void testExamIsModifiedWithValidArgumentsForUpcomingExam(@Mock(name = "exam") final Exam exam) {
        final var examId = TestHelper.validExamId();
        final var newDescription = TestHelper.validExamDescription();
        final var newStartingAt = TestHelper.validExamStartingMoment();
        final var newDuration = TestHelper.validExamDuration();
        doNothing().when(exam).update(newDescription, newStartingAt, newDuration);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examRepository.save(any(Exam.class))).then(invocation -> invocation.getArgument(0));
        Assertions.assertDoesNotThrow(
                () -> examManager.modifyExam(examId, newDescription, newStartingAt, newDuration),
                "An unexpected exception was thrown"
        );
        verify(exam, only()).update(newDescription, newStartingAt, newDuration);
        verify(examRepository, times(1)).findById(examId);
        verify(examRepository, times(1)).save(any(Exam.class));
        verifyNoMoreInteractions(examRepository);
        verifyZeroInteractions(exerciseRepository);
        verifyZeroInteractions(testCaseRepository);
        verifyZeroInteractions(exerciseSolutionRepository);
        verifyZeroInteractions(exerciseSolutionResultRepository);
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }

    /**
     * Tests that starting an upcoming {@link Exam} works as expected
     * (changes the state and then saves the exam instance).
     *
     * @param exam     A mocked {@link Exam} (the one being started).
     * @param exercise A mocked {@link Exercise} (owned by the {@code exam}).
     * @param testCase A mocked {@link TestCase} (owned by the {@code exercise}).
     */
    @Test
    void testExamIsStartedWhenIsUpcomingAndHasExercisesWithPrivateTestCases(
            @Mock(name = "exam") final Exam exam,
            @Mock(name = "exercise") final Exercise exercise,
            @Mock(name = "testCase") final TestCase testCase) {

        final var examId = TestHelper.validExamId();
        doNothing().when(exam).startExam();
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(exerciseRepository.getExamExercises(exam)).thenReturn(List.of(exercise));
        when(testCaseRepository.getExercisePrivateTestCases(exercise)).thenReturn(List.of(testCase));
        when(examRepository.save(any(Exam.class))).then(invocation -> invocation.getArgument(0));
        Assertions.assertDoesNotThrow(
                () -> examManager.startExam(examId),
                "An unexpected exception was thrown"
        );
        verify(exam, only()).startExam();
        verify(examRepository, times(1)).findById(examId);
        verify(examRepository, times(1)).save(exam);
        verifyNoMoreInteractions(examRepository);
        verify(exerciseRepository, only()).getExamExercises(exam);
        verify(testCaseRepository, only()).getExercisePrivateTestCases(exercise);
        verifyZeroInteractions(exerciseSolutionRepository);
        verifyZeroInteractions(exerciseSolutionResultRepository);
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }

    /**
     * Tests that finishing an in progress {@link Exam} works as expected
     * (changes the state and then saves the exam instance).
     *
     * @param exam A mocked {@link Exam} (the one being finished).
     */
    @Test
    void testExamIsFinishedWhenIsInProgress(@Mock(name = "exam") final Exam exam) {
        final var examId = TestHelper.validExamId();
        doNothing().when(exam).finishExam();
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examRepository.save(any(Exam.class))).then(invocation -> invocation.getArgument(0));
        Assertions.assertDoesNotThrow(
                () -> examManager.finishExam(examId),
                "An unexpected exception was thrown"
        );
        verify(exam, only()).finishExam();
        verify(examRepository, times(1)).findById(examId);
        verify(examRepository, times(1)).save(exam);
        verifyNoMoreInteractions(examRepository);
        verifyZeroInteractions(exerciseRepository);
        verifyZeroInteractions(testCaseRepository);
        verifyZeroInteractions(exerciseSolutionRepository);
        verifyZeroInteractions(exerciseSolutionResultRepository);
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }

    /**
     * Tests that deleting an upcoming exam is performed as expected.
     *
     * @param exam A mocked {@link Exam} (the one being deleted).
     */
    @Test
    void testDeleteOfUpcomingExam(@Mock(name = "exam") final Exam exam) {
        final var id = TestHelper.validExamId();
        when(examRepository.findById(id)).thenReturn(Optional.of(exam));
        doNothing().when(examRepository).delete(exam);
        when(exam.getState()).thenReturn(Exam.State.UPCOMING);
        Assertions.assertDoesNotThrow(
                () -> examManager.deleteExam(id),
                "Deleting an exam throws an exception"
        );
        verify(examRepository, times(1)).findById(id);
        verify(examRepository, times(1)).delete(exam);
        verifyNoMoreInteractions(examRepository);
        verify(exerciseRepository, only()).deleteExamExercises(exam);
        verify(testCaseRepository, only()).deleteExamTestCases(exam);
        verifyZeroInteractions(exerciseSolutionRepository);
        verifyZeroInteractions(exerciseSolutionResultRepository);
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }

    /**
     * Tests that the {@link List} of {@link Exercise}s belonging to a given {@link Exam} is returned as expected.
     *
     * @param exam            A mocked {@link Exam} (the owner of the {@link Exercise}s).
     * @param mockedExercises A mocked {@link List} of {@link Exercise}s owned by the {@link Exam}.
     */
    @Test
    void testGetExamExercises(
            @Mock(name = "exam") final Exam exam,
            @Mock(name = "mockedExercises") final List<Exercise> mockedExercises) {
        final var id = TestHelper.validExamId();
        when(examRepository.findById(id)).thenReturn(Optional.of(exam));
        when(exerciseRepository.getExamExercises(exam)).thenReturn(mockedExercises);
        final var exercises = examManager.getExercises(id);
        Assertions.assertEquals(
                mockedExercises,
                exercises,
                "The returned exercises list is not the one returned by the repository"
        );
        verify(examRepository, only()).findById(id);
        verify(exerciseRepository, only()).getExamExercises(exam);
        verifyZeroInteractions(testCaseRepository);
        verifyZeroInteractions(exerciseSolutionRepository);
        verifyZeroInteractions(exerciseSolutionResultRepository);
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }

    /**
     * Tests that clearing an upcoming exam exercises is performed as expected.
     *
     * @param exam A mocked {@link Exam} (the owner of the {@link Exercise}s).
     */
    @Test
    void testClearExercisesOfUpcomingExam(@Mock(name = "exam") final Exam exam) {
        final var examId = TestHelper.validExamId();
        when(exam.getState()).thenReturn(Exam.State.UPCOMING);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        Assertions.assertDoesNotThrow(
                () -> examManager.clearExercises(examId),
                "Clearing exam's exercises throws an exception"
        );
        verify(examRepository, only()).findById(examId);
        verify(exerciseRepository, only()).deleteExamExercises(exam);
        verify(testCaseRepository, only()).deleteExamTestCases(exam);
        verifyZeroInteractions(exerciseSolutionRepository);
        verifyZeroInteractions(exerciseSolutionResultRepository);
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }


    // ================================================================================================================
    // Exercises
    // ================================================================================================================

    /**
     * Tests that creating an exercise for an upcoming exam is performed as expected.
     *
     * @param exam A mocked {@link Exam} (the future owner of the {@link Exercise}).
     */
    @Test
    void testCreateExerciseWithValidArgumentsForUpcomingExam(@Mock(name = "exam") final Exam exam) {
        final var question = TestHelper.validExerciseQuestion();
        final var language = TestHelper.validLanguage();
        final var solutionTemplate = TestHelper.validSolutionTemplate();
        final var awardedScore = TestHelper.validAwardedScore();
        final var examId = TestHelper.validExamId();
        when(exam.getState()).thenReturn(Exam.State.UPCOMING);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(exerciseRepository.save(any(Exercise.class))).then(invocation -> invocation.getArgument(0));
        final var exercise = examManager.createExercise(examId, question, language, solutionTemplate, awardedScore);
        Assertions.assertAll("Exercise properties are not the expected",
                () -> Assertions.assertEquals(
                        question,
                        exercise.getQuestion(),
                        "There is a mismatch in the question"
                ),
                () -> Assertions.assertEquals(
                        language,
                        exercise.getLanguage(),
                        "There is a mismatch in the language"
                ),
                () -> Assertions.assertEquals(
                        solutionTemplate,
                        exercise.getSolutionTemplate(),
                        "There is a mismatch in the solution template"
                ),
                () -> Assertions.assertEquals(
                        awardedScore,
                        exercise.getAwardedScore(),
                        "There is a mismatch in the awarded score"
                ),
                () -> Assertions.assertEquals(
                        exam,
                        exercise.getExam(),
                        "There is a mismatch in the owner"
                )
        );
        verify(exam, only()).getState();
        verify(examRepository, only()).findById(examId);
        verify(exerciseRepository, only()).save(any(Exercise.class));
        verifyZeroInteractions(testCaseRepository);
        verifyZeroInteractions(exerciseSolutionRepository);
        verifyZeroInteractions(exerciseSolutionResultRepository);
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }

    /**
     * Tests that modifying an exercise belonging to an upcoming exam is performed as expected.
     *
     * @param exam     A mocked {@link Exam} (the owner of the exercise).
     * @param exercise A mocked {@link Exercise} (the one being modified).
     */
    @Test
    void testModifyExerciseWithValidArgumentsForUpcomingExam(
            @Mock(name = "exam") final Exam exam,
            @Mock(name = "exercise") final Exercise exercise) {
        final var exerciseId = TestHelper.validExerciseId();
        final var newQuestion = TestHelper.validExerciseQuestion();
        final var newLanguage = TestHelper.validLanguage();
        final var newSolutionTemplate = TestHelper.validSolutionTemplate();
        final var awardedScore = TestHelper.validAwardedScore();
        when(exam.getState()).thenReturn(Exam.State.UPCOMING);
        when(exercise.getExam()).thenReturn(exam);
        doNothing().when(exercise).update(newQuestion, newLanguage, newSolutionTemplate, awardedScore);
        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));
        when(exerciseRepository.save(any(Exercise.class))).then(invocation -> invocation.getArgument(0));
        Assertions.assertDoesNotThrow(
                () -> examManager.modifyExercise(exerciseId, newQuestion, newLanguage, newSolutionTemplate, awardedScore),
                "An unexpected exception was thrown"
        );
        verify(exam, only()).getState();
        verify(exercise, times(1)).getExam();
        verify(exercise, times(1)).update(newQuestion, newLanguage, newSolutionTemplate, awardedScore);
        verifyNoMoreInteractions(exercise);
        verifyZeroInteractions(examRepository);
        verify(exerciseRepository, times(1)).findById(exerciseId);
        verify(exerciseRepository, times(1)).save(exercise);
        verifyNoMoreInteractions(exerciseRepository);
        verifyZeroInteractions(testCaseRepository);
        verifyZeroInteractions(exerciseSolutionRepository);
        verifyZeroInteractions(exerciseSolutionResultRepository);
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }

    /**
     * Tests that deleting an exercise belonging to an upcoming exam is performed as expected.
     *
     * @param exam     A mocked {@link Exam} (the owner of the exercise).
     * @param exercise A mocked {@link Exercise} (the one being deleted).
     */
    @Test
    void testDeleteExerciseBelongingToUpcomingExam(
            @Mock(name = "exam") final Exam exam,
            @Mock(name = "exercise") final Exercise exercise) {
        final var exerciseId = TestHelper.validExerciseId();
        when(exam.getState()).thenReturn(Exam.State.UPCOMING);
        when(exercise.getExam()).thenReturn(exam);
        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));
        doNothing().when(exerciseRepository).delete(exercise);
        Assertions.assertDoesNotThrow(
                () -> examManager.deleteExercise(exerciseId),
                "Deleting an exercise throws an exception"
        );
        verify(exam, only()).getState();
        verify(exercise, only()).getExam();
        verifyZeroInteractions(examRepository);
        verify(exerciseRepository, times(1)).findById(exerciseId);
        verify(exerciseRepository, times(1)).delete(exercise);
        verifyNoMoreInteractions(exerciseRepository);
        verify(testCaseRepository, only()).deleteExerciseTestCases(exercise);
        verifyZeroInteractions(exerciseSolutionRepository);
        verifyZeroInteractions(exerciseSolutionResultRepository);
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }

    /**
     * Tests that listing an exercise's private test cases is performed as expected.
     *
     * @param exercise        A mocked {@link Exercise} (the owner of the {@link TestCase}s).
     * @param mockedTestCases A mocked {@link List} of {@link TestCase}s owned by the {@link Exercise}.
     */
    @Test
    void testListExercisePrivateTestCases(
            @Mock(name = "exercise") final Exercise exercise,
            @Mock(name = "mockedPrivateTestCases") final List<TestCase> mockedTestCases) {
        final var exerciseId = TestHelper.validExerciseId();
        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));
        when(testCaseRepository.getExercisePrivateTestCases(exercise)).thenReturn(mockedTestCases);
        final var testCases = examManager.getPrivateTestCases(exerciseId);
        Assertions.assertEquals(
                mockedTestCases,
                testCases,
                "The returned test cases list is not the one returned by the repository"
        );
        verifyZeroInteractions(examRepository);
        verify(exerciseRepository, only()).findById(exerciseId);
        verify(testCaseRepository, only()).getExercisePrivateTestCases(exercise);
        verifyZeroInteractions(exerciseSolutionRepository);
        verifyZeroInteractions(exerciseSolutionResultRepository);
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }

    /**
     * Tests that listing an exercise's public test cases is performed as expected.
     *
     * @param exercise        A mocked {@link Exercise} (the owner of the {@link TestCase}s).
     * @param mockedTestCases A mocked {@link List} of {@link TestCase}s owned by the {@link Exercise}.
     */
    @Test
    void testListExercisePublicTestCases(
            @Mock(name = "exercise") final Exercise exercise,
            @Mock(name = "mockedPublicTestCases") final List<TestCase> mockedTestCases) {
        final var exerciseId = TestHelper.validExerciseId();
        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));
        when(testCaseRepository.getExercisePublicTestCases(exercise)).thenReturn(mockedTestCases);
        final var testCases = examManager.getPublicTestCases(exerciseId);
        Assertions.assertEquals(
                mockedTestCases,
                testCases,
                "The returned test cases list is not the one returned by the repository"
        );
        verifyZeroInteractions(examRepository);
        verify(exerciseRepository, only()).findById(exerciseId);
        verify(testCaseRepository, only()).getExercisePublicTestCases(exercise);
        verifyZeroInteractions(exerciseSolutionRepository);
        verifyZeroInteractions(exerciseSolutionResultRepository);
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }


    /**
     * Tests that listing an exercise's solutions is performed as expected.
     *
     * @param exercise           A mocked {@link Exercise} (the owner of the {@link TestCase}s).
     * @param pagingRequest      A mocked {@link PagingRequest} to be passed to the {@link ExerciseSolutionRepository}.
     * @param mockedExeSolutions A mocked {@link Page} of {@link ExerciseSolution}s belonging to the {@link Exercise}.
     */
    @Test
    void testListExerciseSolutions(
            @Mock(name = "exercise") final Exercise exercise,
            @Mock(name = "pagingRequest") final PagingRequest pagingRequest,
            @Mock(name = "mockedSolutions") final Page<ExerciseSolution> mockedExeSolutions) {
        final var exerciseId = TestHelper.validExerciseId();
        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));
        when(exerciseSolutionRepository.getExerciseSolutions(exercise, pagingRequest)).thenReturn(mockedExeSolutions);
        final var solutions = examManager.listSolutions(exerciseId, pagingRequest);
        Assertions.assertEquals(
                mockedExeSolutions,
                solutions,
                "The returned solutions is not the one returned by the repository"
        );
        verifyZeroInteractions(examRepository);
        verify(exerciseRepository, only()).findById(exerciseId);
        verifyZeroInteractions(testCaseRepository);
        verify(exerciseSolutionRepository, only()).getExerciseSolutions(exercise, pagingRequest);
        verifyZeroInteractions(exerciseSolutionResultRepository);
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }


    // ================================================================================================================
    // Test Cases
    // ================================================================================================================

    /**
     * Tests that creating a test case for an exercise of an upcoming exam is performed as expected.
     *
     * @param exam     A mocked {@link Exam} (the owner of the {@link Exercise}s).
     * @param exercise A mocked {@link Exercise} (the future owner of the {@link TestCase}).
     */
    @Test
    void testCreateTestCaseWithValidArgumentsForExerciseOfUpcomingExam(
            @Mock(name = "exam") final Exam exam,
            @Mock(name = "exercise") final Exercise exercise) {
        final var visibility = TestHelper.validTestCaseVisibility();
        final var timeout = TestHelper.validTestCaseTimeout();
        final var inputs = TestHelper.validTestCaseList();
        final var expectedOutputs = TestHelper.validTestCaseList();
        final var exerciseId = TestHelper.validExerciseId();
        when(exam.getState()).thenReturn(Exam.State.UPCOMING);
        when(exercise.getExam()).thenReturn(exam);
        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));
        when(testCaseRepository.save(any(TestCase.class))).then(invocation -> invocation.getArgument(0));
        final var testCase = examManager.createTestCase(exerciseId, visibility, timeout, inputs, expectedOutputs);
        Assertions.assertAll("TestCase properties are not the expected",
                () -> Assertions.assertEquals(
                        visibility,
                        testCase.getVisibility(),
                        "There is a mismatch in the visibility"
                ),
                () -> Assertions.assertEquals(
                        timeout,
                        testCase.getTimeout(),
                        "There is a mismatch in the timeout"
                ),
                () -> Assertions.assertEquals(
                        inputs,
                        testCase.getInputs(),
                        "There is a mismatch in the inputs"
                ),
                () -> Assertions.assertEquals(
                        expectedOutputs,
                        testCase.getExpectedOutputs(),
                        "There is a mismatch in the expected outputs"
                ),
                () -> Assertions.assertEquals(
                        exercise,
                        testCase.getExercise(),
                        "There is a mismatch in the owner"
                )
        );
        verify(exam, only()).getState();
        verify(exercise, only()).getExam();
        verifyZeroInteractions(examRepository);
        verify(exerciseRepository, only()).findById(exerciseId);
        verify(testCaseRepository, only()).save(any(TestCase.class));
        verifyZeroInteractions(exerciseSolutionRepository);
        verifyZeroInteractions(exerciseSolutionResultRepository);
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }

    /**
     * Tests that modifying a test case belonging to an exercise of an upcoming exam is performed as expected.
     *
     * @param exam     A mocked {@link Exam} (the owner of the exercise).
     * @param exercise A mocked {@link Exercise} (the owner of the test case)
     * @param testCase A mocked {@link TestCase} (the one being modified).
     */
    @Test
    void testModifyTestCaseWithValidValuesForExerciseOfUpcomingExam(
            @Mock(name = "exam") final Exam exam,
            @Mock(name = "exercise") final Exercise exercise,
            @Mock(name = "testCase") final TestCase testCase) {
        final var testCaseId = TestHelper.validTestCaseId();
        final var newVisibility = TestHelper.validTestCaseVisibility();
        final var newTimeout = TestHelper.validTestCaseTimeout();
        final var newInputs = TestHelper.validTestCaseList();
        final var newExpectedOutputs = TestHelper.validTestCaseList();
        when(exam.getState()).thenReturn(Exam.State.UPCOMING);
        when(exercise.getExam()).thenReturn(exam);
        when(testCase.getExercise()).thenReturn(exercise);
        doNothing().when(testCase).update(newVisibility, newTimeout, newInputs, newExpectedOutputs);
        when(testCaseRepository.findById(testCaseId)).thenReturn(Optional.of(testCase));
        when(testCaseRepository.save(any(TestCase.class))).then(inv -> inv.getArgument(0));
        Assertions.assertDoesNotThrow(
                () -> examManager.modifyTestCase(testCaseId, newVisibility, newTimeout, newInputs, newExpectedOutputs),
                "An unexpected exception was thrown"
        );
        verify(exam, only()).getState();
        verify(exercise, only()).getExam();
        verify(testCase, times(1)).getExercise();
        verify(testCase, times(1)).update(newVisibility, newTimeout, newInputs, newExpectedOutputs);
        verifyNoMoreInteractions(testCase);
        verifyZeroInteractions(examRepository);
        verifyZeroInteractions(exerciseRepository);
        verify(testCaseRepository, times(1)).findById(testCaseId);
        verify(testCaseRepository, times(1)).save(testCase);
        verifyNoMoreInteractions(testCaseRepository);
        verifyZeroInteractions(exerciseSolutionRepository);
        verifyZeroInteractions(exerciseSolutionResultRepository);
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }

    /**
     * Tests that deleting a test case of an exercise belonging to an upcoming exam is performed as expected.
     *
     * @param exam     A mocked {@link Exam} (the owner of the exercise).
     * @param exercise A mocked {@link Exercise} (the owner of the test case)
     * @param testCase A mocked {@link TestCase} (the one being deleted).
     */
    @Test
    void testDeleteTestCaseOfExerciseBelongingToUpcomingExam(
            @Mock(name = "exam") final Exam exam,
            @Mock(name = "exercise") final Exercise exercise,
            @Mock(name = "testCase") final TestCase testCase) {
        final var testCaseId = TestHelper.validTestCaseId();
        when(exam.getState()).thenReturn(Exam.State.UPCOMING);
        when(exercise.getExam()).thenReturn(exam);
        when(testCase.getExercise()).thenReturn(exercise);
        when(testCaseRepository.findById(testCaseId)).thenReturn(Optional.of(testCase));
        doNothing().when(testCaseRepository).delete(testCase);
        Assertions.assertDoesNotThrow(
                () -> examManager.deleteTestCase(testCaseId),
                "Deleting a test case throws an exception"
        );
        verify(exam, only()).getState();
        verify(exercise, only()).getExam();
        verify(testCase, only()).getExercise();
        verifyZeroInteractions(examRepository);
        verifyZeroInteractions(exerciseRepository);
        verify(testCaseRepository, times(1)).findById(testCaseId);
        verify(testCaseRepository, times(1)).delete(testCase);
        verifyNoMoreInteractions(testCaseRepository);
        verifyZeroInteractions(exerciseSolutionRepository);
        verifyZeroInteractions(exerciseSolutionResultRepository);
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }


    // ================================================================================================================
    // Solutions
    // ================================================================================================================

    /**
     * Tests that creating a solution for an exercise belonging to an in progress exam is performed as expected.
     *
     * @param exam     A mocked {@link Exercise} (the owner of the {@link Exercise}s).
     * @param exercise A mocked {@link Exercise} (the future owner of the {@link ExerciseSolution}).
     */
    @Test
    void testCreateSolutionForExerciseBelongingToInProgressExam(
            @Mock(name = "exam") final Exam exam,
            @Mock(name = "exercise") final Exercise exercise) {
        final var answer = TestHelper.validExerciseSolutionAnswer();
        final var language = TestHelper.validLanguage();
        final var exerciseId = TestHelper.validExerciseId();
        when(exam.getState()).thenReturn(Exam.State.IN_PROGRESS);
        when(exercise.getExam()).thenReturn(exam);
        when(exercise.getLanguage()).thenReturn(language);
        when(exerciseRepository.findById(exerciseId)).thenReturn(Optional.of(exercise));
        when(exerciseSolutionRepository.save(any(ExerciseSolution.class))).then(invocation -> invocation.getArgument(0));
        final var privateTestCases = mockTestCases();
        final var publicTestCases = mockTestCases();
        final var allTestCases = Stream.concat(
                privateTestCases.stream(),
                publicTestCases.stream()
        ).collect(Collectors.toList());
        when(testCaseRepository.getExercisePrivateTestCases(exercise)).thenReturn(privateTestCases);
        when(testCaseRepository.getExercisePublicTestCases(exercise)).thenReturn(publicTestCases);
        final var solution = examManager.createExerciseSolution(exerciseId, answer);
        Assertions.assertAll("ExerciseSolution properties are not the expected",
                () -> Assertions.assertEquals(
                        answer,
                        solution.getAnswer(),
                        "There is a mismatch in the answer"
                ),
                () -> Assertions.assertEquals(
                        exercise,
                        solution.getExercise(),
                        "There is a mismatch in the owner"
                )

        );
        verify(exam, only()).getState();
        verify(exercise, times(1)).getExam();
        verify(exercise, times(allTestCases.size())).getLanguage(); // Call per test case to be sent to run
        verifyNoMoreInteractions(exercise);
        verifyZeroInteractions(examRepository);
        verify(exerciseRepository, only()).findById(exerciseId);
        verify(testCaseRepository, times(1)).getExercisePrivateTestCases(exercise);
        verify(testCaseRepository, times(1)).getExercisePublicTestCases(exercise);
        verifyNoMoreInteractions(testCaseRepository);
        verify(exerciseSolutionRepository, only()).save(any(ExerciseSolution.class));
        verifyZeroInteractions(exerciseSolutionResultRepository);
        allTestCases.forEach(testCase -> {
            final var request = new ExecutionRequest(answer, testCase.getInputs(), testCase.getTimeout(), language);
            final var replyData = new ExecutionResultReplyData(solution.getId(), testCase.getId());
            verify(executorServiceCommandMessageProxy, times(1))
                    .requestExecution(request, replyData);
        });
        verifyNoMoreInteractions(executorServiceCommandMessageProxy);
    }


    // ================================================================================================================
    // Solution Results
    // ================================================================================================================

    /**
     * Tests the processing of an execution result
     * when the {@link ExecutionResult} is a {@link FinishedExecutionResult} with a non zero exit code.
     *
     * @param exerciseSolution The {@link ExerciseSolution} being executed.
     * @param testCase         The {@link TestCase} used in the execution.
     * @param executionResult  The {@link FinishedExecutionResult} being analyzed.
     */
    @Test
    void testProcessExecutionWithFinishedExecutionResultWithNonZeroExitCode(
            @Mock(name = "solution") final ExerciseSolution exerciseSolution,
            @Mock(name = "testCase") final TestCase testCase,
            @Mock(name = "executionResult") final FinishedExecutionResult executionResult) {
        final var testCaseId = TestHelper.validTestCaseId();
        final var solutionId = TestHelper.validExerciseSolutionId();

        when(executionResult.getExitCode()).thenReturn(TestHelper.validNonZeroExerciseSolutionExitCode());
        when(testCaseRepository.findById(testCaseId)).thenReturn(Optional.of(testCase));
        when(exerciseSolutionRepository.findById(solutionId)).thenReturn(Optional.of(exerciseSolution));
        when(exerciseSolutionResultRepository.save(any(ExerciseSolutionResult.class))).then(i -> i.getArgument(0));
        Assertions.assertDoesNotThrow(
                () -> examManager.processExecution(solutionId, testCaseId, executionResult),
                "An exception was thrown when processing a finished execution result with a non zero exit code"
        );

        verifyZeroInteractions(examRepository);
        verifyZeroInteractions(exerciseRepository);
        verify(testCaseRepository, only()).findById(testCaseId);
        verify(exerciseSolutionRepository, only()).findById(solutionId);
        verify(exerciseSolutionResultRepository, only())
                .save(argThat(withResult(ExerciseSolutionResult.Result.FAILED)))
        ;
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }

    /**
     * Tests the processing of an execution result
     * when the {@link ExecutionResult} is a {@link FinishedExecutionResult} with a zero exit code,
     * but a non empty standard error output.
     *
     * @param exerciseSolution The {@link ExerciseSolution} being executed.
     * @param testCase         The {@link TestCase} used in the execution.
     * @param executionResult  The {@link FinishedExecutionResult} being analyzed.
     */
    @Test
    void testProcessExecutionWithFinishedExecutionResultWithZeroExitCodeAndNonEmptyStderr(
            @Mock(name = "solution") final ExerciseSolution exerciseSolution,
            @Mock(name = "testCase") final TestCase testCase,
            @Mock(name = "executionResult") final FinishedExecutionResult executionResult) {
        final var testCaseId = TestHelper.validTestCaseId();
        final var solutionId = TestHelper.validExerciseSolutionId();
        final var stderr = TestHelper.validExerciseSolutionResultList();

        when(executionResult.getExitCode()).thenReturn(0);
        when(executionResult.getStderr()).thenReturn(stderr);
        when(testCaseRepository.findById(testCaseId)).thenReturn(Optional.of(testCase));
        when(exerciseSolutionRepository.findById(solutionId)).thenReturn(Optional.of(exerciseSolution));
        when(exerciseSolutionResultRepository.save(any(ExerciseSolutionResult.class))).then(i -> i.getArgument(0));
        Assertions.assertDoesNotThrow(
                () -> examManager.processExecution(solutionId, testCaseId, executionResult),
                "An exception was thrown when processing a finished execution result with a non empty stderr list"
        );

        verifyZeroInteractions(examRepository);
        verifyZeroInteractions(exerciseRepository);
        verify(testCaseRepository, only()).findById(testCaseId);
        verify(exerciseSolutionRepository, only()).findById(solutionId);
        verify(exerciseSolutionResultRepository, only())
                .save(argThat(withResult(ExerciseSolutionResult.Result.FAILED)))
        ;
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }

    /**
     * Tests the processing of an execution result
     * when the {@link ExecutionResult} is a {@link FinishedExecutionResult} with a non zero exit code,
     * no standard error output and standard output equal to the expected output.
     *
     * @param exerciseSolution The {@link ExerciseSolution} being executed.
     * @param testCase         The {@link TestCase} used in the execution.
     * @param executionResult  The {@link FinishedExecutionResult} being analyzed.
     */
    @Test
    void testProcessExecutionWithFinishedExecutionResultWithZeroExitCodeEmptyStderrAndDifferentOutput(
            @Mock(name = "solution") final ExerciseSolution exerciseSolution,
            @Mock(name = "testCase") final TestCase testCase,
            @Mock(name = "executionResult") final FinishedExecutionResult executionResult) {
        final var testCaseId = TestHelper.validTestCaseId();
        final var solutionId = TestHelper.validExerciseSolutionId();
        final var outputs = TestHelper.validExerciseSolutionResultList();
        final var anotherOutputs = new LinkedList<>(outputs);
        Collections.shuffle(anotherOutputs);

        when(testCase.getExpectedOutputs()).thenReturn(outputs);
        when(executionResult.getExitCode()).thenReturn(0);
        when(executionResult.getStderr()).thenReturn(Collections.emptyList());
        when(executionResult.getStdout()).thenReturn(anotherOutputs);
        when(testCaseRepository.findById(testCaseId)).thenReturn(Optional.of(testCase));
        when(exerciseSolutionRepository.findById(solutionId)).thenReturn(Optional.of(exerciseSolution));
        when(exerciseSolutionResultRepository.save(any(ExerciseSolutionResult.class))).then(i -> i.getArgument(0));
        Assertions.assertDoesNotThrow(
                () -> examManager.processExecution(solutionId, testCaseId, executionResult),
                "An exception was thrown when processing a finished execution result with not expected outputs"
        );

        verifyZeroInteractions(examRepository);
        verifyZeroInteractions(exerciseRepository);
        verify(testCaseRepository, only()).findById(testCaseId);
        verify(exerciseSolutionRepository, only()).findById(solutionId);
        verify(exerciseSolutionResultRepository, only())
                .save(argThat(withResult(ExerciseSolutionResult.Result.FAILED)))
        ;
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }

    /**
     * Tests the processing of an execution result
     * when the {@link ExecutionResult} is a {@link FinishedExecutionResult} with a non zero exit code,
     * no standard error output and standard output equal to the expected output.
     *
     * @param exerciseSolution The {@link ExerciseSolution} being executed.
     * @param testCase         The {@link TestCase} used in the execution.
     * @param executionResult  The {@link FinishedExecutionResult} being analyzed.
     */
    @Test
    void testProcessExecutionWithFinishedExecutionResultWithZeroExitCodeAndEmptyStderrAndExpectedOutput(
            @Mock(name = "solution") final ExerciseSolution exerciseSolution,
            @Mock(name = "testCase") final TestCase testCase,
            @Mock(name = "executionResult") final FinishedExecutionResult executionResult) {
        final var testCaseId = TestHelper.validTestCaseId();
        final var solutionId = TestHelper.validExerciseSolutionId();
        final var outputs = TestHelper.validExerciseSolutionResultList();

        when(testCase.getExpectedOutputs()).thenReturn(outputs);
        when(executionResult.getExitCode()).thenReturn(0);
        when(executionResult.getStderr()).thenReturn(Collections.emptyList());
        when(executionResult.getStdout()).thenReturn(outputs);

        when(testCaseRepository.findById(testCaseId)).thenReturn(Optional.of(testCase));
        when(exerciseSolutionRepository.findById(solutionId)).thenReturn(Optional.of(exerciseSolution));
        when(exerciseSolutionResultRepository.save(any(ExerciseSolutionResult.class))).then(i -> i.getArgument(0));
        Assertions.assertDoesNotThrow(
                () -> examManager.processExecution(solutionId, testCaseId, executionResult),
                "An exception was thrown when processing a finished execution result with the expected outputs"
        );

        verifyZeroInteractions(examRepository);
        verifyZeroInteractions(exerciseRepository);
        verify(testCaseRepository, only()).findById(testCaseId);
        verify(exerciseSolutionRepository, only()).findById(solutionId);
        verify(exerciseSolutionResultRepository, only())
                .save(argThat(withResult(ExerciseSolutionResult.Result.APPROVED)))
        ;
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }

    /**
     * Tests the processing of an execution result
     * when the {@link ExecutionResult} is a {@link TimedOutExecutionResult}.
     *
     * @param exerciseSolution The {@link ExerciseSolution} being executed.
     * @param testCase         The {@link TestCase} used in the execution.
     * @param executionResult  The {@link TimedOutExecutionResult} being analyzed.
     */
    @Test
    void testProcessExecutionWithTimedOutExecutionResult(
            @Mock(name = "solution") final ExerciseSolution exerciseSolution,
            @Mock(name = "testCase") final TestCase testCase,
            @Mock(name = "executionResult") final TimedOutExecutionResult executionResult) {
        final var testCaseId = TestHelper.validTestCaseId();
        final var solutionId = TestHelper.validExerciseSolutionId();

        when(testCaseRepository.findById(testCaseId)).thenReturn(Optional.of(testCase));
        when(exerciseSolutionRepository.findById(solutionId)).thenReturn(Optional.of(exerciseSolution));
        when(exerciseSolutionResultRepository.save(any(ExerciseSolutionResult.class))).then(i -> i.getArgument(0));
        Assertions.assertDoesNotThrow(
                () -> examManager.processExecution(solutionId, testCaseId, executionResult),
                "An exception was thrown when processing a timed-out execution result"
        );

        verifyZeroInteractions(examRepository);
        verifyZeroInteractions(exerciseRepository);
        verify(testCaseRepository, only()).findById(testCaseId);
        verify(exerciseSolutionRepository, only()).findById(solutionId);
        verify(exerciseSolutionResultRepository, only())
                .save(argThat(withResult(ExerciseSolutionResult.Result.TIMED_OUT)))
        ;
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }

    /**
     * Tests the processing of an execution result
     * when the {@link ExecutionResult} is a {@link CompileErrorExecutionResult}.
     *
     * @param exerciseSolution The {@link ExerciseSolution} being executed.
     * @param testCase         The {@link TestCase} used in the execution.
     * @param executionResult  The {@link TimedOutExecutionResult} being analyzed.
     */
    @Test
    void testProcessExecutionWithNotCompiledExecutionResult(
            @Mock(name = "solution") final ExerciseSolution exerciseSolution,
            @Mock(name = "testCase") final TestCase testCase,
            @Mock(name = "executionResult") final CompileErrorExecutionResult executionResult) {
        final var testCaseId = TestHelper.validTestCaseId();
        final var solutionId = TestHelper.validExerciseSolutionId();

        when(testCaseRepository.findById(testCaseId)).thenReturn(Optional.of(testCase));
        when(exerciseSolutionRepository.findById(solutionId)).thenReturn(Optional.of(exerciseSolution));
        when(exerciseSolutionResultRepository.save(any(ExerciseSolutionResult.class))).then(i -> i.getArgument(0));
        Assertions.assertDoesNotThrow(
                () -> examManager.processExecution(solutionId, testCaseId, executionResult),
                "An exception was thrown when processing a timed-out execution result"
        );

        verifyZeroInteractions(examRepository);
        verifyZeroInteractions(exerciseRepository);
        verify(testCaseRepository, only()).findById(testCaseId);
        verify(exerciseSolutionRepository, only()).findById(solutionId);
        verify(exerciseSolutionResultRepository, only())
                .save(argThat(withResult(ExerciseSolutionResult.Result.NOT_COMPILED)))
        ;
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }

    /**
     * Tests the processing of an execution result
     * when the {@link ExecutionResult} is an {@link InitializationErrorExecutionResult}.
     *
     * @param exerciseSolution The {@link ExerciseSolution} being executed.
     * @param testCase         The {@link TestCase} used in the execution.
     * @param executionResult  The {@link TimedOutExecutionResult} being analyzed.
     */
    @Test
    void testProcessExecutionWithInitializationErrorExecutionResult(
            @Mock(name = "solution") final ExerciseSolution exerciseSolution,
            @Mock(name = "testCase") final TestCase testCase,
            @Mock(name = "executionResult") final InitializationErrorExecutionResult executionResult) {
        final var testCaseId = TestHelper.validTestCaseId();
        final var solutionId = TestHelper.validExerciseSolutionId();

        when(testCaseRepository.findById(testCaseId)).thenReturn(Optional.of(testCase));
        when(exerciseSolutionRepository.findById(solutionId)).thenReturn(Optional.of(exerciseSolution));
        Assertions.assertDoesNotThrow(
                () -> examManager.processExecution(solutionId, testCaseId, executionResult),
                "An exception was thrown when processing a timed-out execution result"
        );

        verifyZeroInteractions(examRepository);
        verifyZeroInteractions(exerciseRepository);
        verify(testCaseRepository, only()).findById(testCaseId);
        verify(exerciseSolutionRepository, only()).findById(solutionId);
        verifyZeroInteractions(exerciseSolutionResultRepository);
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }

    /**
     * Tests the processing of an execution result
     * when the {@link ExecutionResult} is an {@link InitializationErrorExecutionResult}.
     *
     * @param exerciseSolution The {@link ExerciseSolution} being executed.
     * @param testCase         The {@link TestCase} used in the execution.
     * @param executionResult  The {@link TimedOutExecutionResult} being analyzed.
     */
    @Test
    void testProcessExecutionWithUnknownErrorExecutionResult(
            @Mock(name = "solution") final ExerciseSolution exerciseSolution,
            @Mock(name = "testCase") final TestCase testCase,
            @Mock(name = "executionResult") final UnknownErrorExecutionResult executionResult) {
        final var testCaseId = TestHelper.validTestCaseId();
        final var solutionId = TestHelper.validExerciseSolutionId();

        when(testCaseRepository.findById(testCaseId)).thenReturn(Optional.of(testCase));
        when(exerciseSolutionRepository.findById(solutionId)).thenReturn(Optional.of(exerciseSolution));
        Assertions.assertDoesNotThrow(
                () -> examManager.processExecution(solutionId, testCaseId, executionResult),
                "An exception was thrown when processing a timed-out execution result"
        );

        verifyZeroInteractions(examRepository);
        verifyZeroInteractions(exerciseRepository);
        verify(testCaseRepository, only()).findById(testCaseId);
        verify(exerciseSolutionRepository, only()).findById(solutionId);
        verifyZeroInteractions(exerciseSolutionResultRepository);
        verifyZeroInteractions(executorServiceCommandMessageProxy);
    }


    // ================================================================================================================
    // Helpers
    // ================================================================================================================

    /**
     * Creates a {@link List} of mocked {@link TestCase}s.
     *
     * @return A {@link List} of mocked {@link TestCase}s.
     */
    private static List<TestCase> mockTestCases() {
        return TestHelper
                .randomLengthStream(ignored -> mock(TestCase.class))
                .peek(mock -> when(mock.getId()).thenReturn(TestHelper.validTestCaseId()))
                .peek(mock -> when(mock.getTimeout()).thenReturn(TestHelper.validTestCaseTimeout()))
                .peek(mock -> when(mock.getInputs()).thenReturn(TestHelper.validTestCaseList()))
                .collect(Collectors.toList());
    }

    /**
     * Creates an {@link ArgumentMatcher} of {@link ExerciseSolutionResult} that expects the
     * {@link ExerciseSolutionResult} has a {@code result} property whose value is the given {@code result}.
     *
     * @param result The expected {@link ExerciseSolutionResult.Result}.
     * @return The created {@link ArgumentMatcher} of {@link ExerciseSolutionResult}.
     */
    private static ArgumentMatcher<ExerciseSolutionResult> withResult(final ExerciseSolutionResult.Result result) {
        return new HamcrestArgumentMatcher<>(Matchers.hasProperty("result", Matchers.equalTo(result)));
    }
}
