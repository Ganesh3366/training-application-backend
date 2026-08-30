package com.ganesh.training_application_backend.course;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Long> {

	List<AnswerOption> findByQuestionIdOrderByPositionAsc(Long questionId);

	List<AnswerOption> findByQuestionIdInOrderByQuestionIdAscPositionAsc(Collection<Long> questionIds);

	Optional<AnswerOption> findByIdAndQuestionId(Long id, Long questionId);

	Optional<AnswerOption> findTopByQuestionIdOrderByPositionDesc(Long questionId);

	List<AnswerOption> findByQuestionIdAndCorrectTrue(Long questionId);

	void deleteAllByQuestionId(Long questionId);

	void deleteAllByQuestionIdIn(Collection<Long> questionIds);
}
