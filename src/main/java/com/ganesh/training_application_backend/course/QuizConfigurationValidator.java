package com.ganesh.training_application_backend.course;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class QuizConfigurationValidator {

	public void requireUsable(List<QuizQuestion> questions, Map<Long, List<AnswerOption>> optionsByQuestion) {
		boolean unusable = questions.isEmpty() || questions.stream().anyMatch(question -> {
			List<AnswerOption> options = optionsByQuestion.getOrDefault(question.getId(), List.of());
			return options.size() < 2 || options.stream().filter(AnswerOption::isCorrect).count() != 1;
		});
		if (unusable) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Quiz configuration is incomplete");
		}
	}
}
