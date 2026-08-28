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
@Table(name = "answer_options")
public class AnswerOption {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id", nullable = false)
	private QuizQuestion question;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String optionText;

	@Column(nullable = false)
	private boolean correct;

	@Column(nullable = false)
	private Integer position;

	protected AnswerOption() {
	}

	public AnswerOption(Long id, QuizQuestion question, String optionText, boolean correct, Integer position) {
		this.id = id;
		this.question = question;
		this.optionText = optionText;
		this.correct = correct;
		this.position = position;
	}

	public Long getId() {
		return id;
	}

	public QuizQuestion getQuestion() {
		return question;
	}

	public String getOptionText() {
		return optionText;
	}

	public boolean isCorrect() {
		return correct;
	}

	public Integer getPosition() {
		return position;
	}
}
