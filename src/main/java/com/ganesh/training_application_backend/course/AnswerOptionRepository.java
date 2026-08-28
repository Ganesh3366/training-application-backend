package com.ganesh.training_application_backend.course;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Long> {

	List<AnswerOption> findByQuestionIdOrderByPositionAsc(Long questionId);

	List<AnswerOption> findByQuestionIdInOrderByQuestionIdAscPositionAsc(Collection<Long> questionIds);
}
