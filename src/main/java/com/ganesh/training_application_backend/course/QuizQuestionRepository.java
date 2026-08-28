package com.ganesh.training_application_backend.course;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

	List<QuizQuestion> findByQuizIdOrderByPositionAsc(Long quizId);
}
