package com.ganesh.training_application_backend.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quiz_id", nullable = false)
	private Quiz quiz;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String questionText;

	@Column(nullable = false)
	private Integer position;

	protected QuizQuestion() {
	}

	public QuizQuestion(Long id, Quiz quiz, String questionText, Integer position) {
		this.id = id;
		this.quiz = quiz;
		this.questionText = questionText;
		this.position = position;
	}

	public Long getId() {
		return id;
	}

	public Quiz getQuiz() {
		return quiz;
	}

	public String getQuestionText() {
		return questionText;
	}

	public Integer getPosition() {
		return position;
	}
}
