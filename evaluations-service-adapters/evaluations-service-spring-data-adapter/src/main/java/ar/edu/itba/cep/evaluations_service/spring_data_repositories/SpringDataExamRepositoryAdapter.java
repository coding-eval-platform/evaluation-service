package ar.edu.itba.cep.evaluations_service.spring_data_repositories;

import ar.edu.itba.cep.evaluations_service.models.Exam;
import ar.edu.itba.cep.evaluations_service.repositories.ExamRepository;
import ar.edu.itba.cep.evaluations_service.spring_data_repositories.spring_data_interfaces.SpringDataExamRepository;
import com.bellotapps.webapps_commons.persistence.repository_utils.Page;
import com.bellotapps.webapps_commons.persistence.repository_utils.PagingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * A concrete implementation of a {@link ExamRepository}
 * which acts as an adapter for a {@link SpringDataExamRepositoryAdapter}.
 */
@Repository
public class SpringDataExamRepositoryAdapter implements ExamRepository {

    /**
     * A {@link SpringDataExamRepository} to which all operations are delegated.
     */
    private final SpringDataExamRepository repository;

    /**
     * Constructor.
     *
     * @param repository A {@link SpringDataExamRepository} to which all operations are delegated.
     */
    @Autowired
    public SpringDataExamRepositoryAdapter(final SpringDataExamRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<Exam> findAll(final PagingRequest pagingRequest) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public long count() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean existsById(final Long aLong) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Exam> findById(final Long aLong) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Iterable<Exam> findAll() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public <S extends Exam> S save(final S entity) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public <S extends Exam> void delete(final S entity) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void deleteById(final Long aLong) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}