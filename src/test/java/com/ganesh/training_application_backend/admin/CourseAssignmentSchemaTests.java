package com.ganesh.training_application_backend.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Table;

class CourseAssignmentSchemaTests {

	@Test
	void declaresDatabaseUniqueConstraintForUserAndCourse() {
		Table table = CourseAssignment.class.getAnnotation(Table.class);

		assertThat(table).isNotNull();
		assertThat(Arrays.asList(table.uniqueConstraints()))
				.anySatisfy(constraint -> {
					assertThat(constraint.name()).isEqualTo(CourseAssignment.USER_COURSE_CONSTRAINT);
					assertThat(constraint.columnNames()).containsExactly("user_id", "course_id");
				});
	}
}
