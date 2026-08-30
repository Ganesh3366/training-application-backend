package com.ganesh.training_application_backend.course;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

	List<QuizQuestion> findByQuizIdOrderByPositionAsc(Long quizId);

	Optional<QuizQuestion> findByIdAndQuizId(Long id, Long quizId);

	Optional<QuizQuestion> findTopByQuizIdOrderByPositionDesc(Long quizId);

	void deleteAllByQuizId(Long quizId);
}
