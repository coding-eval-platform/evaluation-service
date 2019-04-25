package ar.edu.itba.cep.evaluations_service.spring_data.interfaces;

import ar.edu.itba.cep.evaluations_service.models.Exam;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * A repository for {@link Exam}s.
 */
@Repository
public interface SpringDataExamRepository extends PagingAndSortingRepository<Exam, Long> {
}