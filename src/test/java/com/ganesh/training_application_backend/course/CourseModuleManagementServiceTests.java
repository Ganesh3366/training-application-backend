package com.ganesh.training_application_backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.course.dto.CourseModuleCreateRequest;
import com.ganesh.training_application_backend.course.dto.CourseModuleUpdateRequest;
import com.ganesh.training_application_backend.course.dto.ModuleContentCreateRequest;
import com.ganesh.training_application_backend.course.dto.ModuleContentUpdateRequest;

@ExtendWith(MockitoExtension.class)
class CourseModuleManagementServiceTests {

	@Mock CourseRepository courseRepository;
	@Mock CourseModuleRepository courseModuleRepository;
	@Mock ModuleContentRepository moduleContentRepository;
	@Mock QuizRepository quizRepository;
	@Mock ModuleProgressRepository moduleProgressRepository;
	@InjectMocks CourseModuleManagementService service;

	private Course course;
	private CourseModule module;

	@BeforeEach
	void setUp() {
		course = new Course(1L, "Course", "Description", "Instructor", 60,
				CourseLevel.BEGINNER, CourseCategory.INFORMATION_TECHNOLOGY);
		module = new CourseModule(10L, course, "Module", "Description", 3);
	}

	@Test
	void moduleCreationStartsAtOneAndAppendsAfterMaximum() {
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(courseModuleRepository.save(any(CourseModule.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.createModule(1L, new CourseModuleCreateRequest(" First ", " Description "));
		ArgumentCaptor<CourseModule> captor = ArgumentCaptor.forClass(CourseModule.class);
		verify(courseModuleRepository).save(captor.capture());
		assertThat(captor.getValue().getPosition()).isEqualTo(1);
		assertThat(captor.getValue().getTitle()).isEqualTo("First");

		when(courseModuleRepository.findTopByCourseIdOrderByPositionDesc(1L)).thenReturn(Optional.of(module));
		service.createModule(1L, new CourseModuleCreateRequest("Next", null));
		verify(courseModuleRepository, org.mockito.Mockito.times(2)).save(captor.capture());
		assertThat(captor.getAllValues().get(2).getPosition()).isEqualTo(4);
	}

	@Test
	void moduleUpdatePreservesPosition() {
		when(courseModuleRepository.findByIdAndCourseId(10L, 1L)).thenReturn(Optional.of(module));

		var response = service.updateModule(1L, 10L, new CourseModuleUpdateRequest(" Updated ", null));

		assertThat(response.title()).isEqualTo("Updated");
		assertThat(response.position()).isEqualTo(3);
	}

	@Test
	void contentCreationStartsAtOneAndAppendsAfterMaximum() {
		when(courseModuleRepository.findByIdAndCourseId(10L, 1L)).thenReturn(Optional.of(module));
		when(moduleContentRepository.save(any(ModuleContent.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.createContent(1L, 10L,
				new ModuleContentCreateRequest(ModuleContentType.TEXT, " Text ", " Body ", null));
		ArgumentCaptor<ModuleContent> captor = ArgumentCaptor.forClass(ModuleContent.class);
		verify(moduleContentRepository).save(captor.capture());
		assertThat(captor.getValue().getPosition()).isEqualTo(1);
		assertThat(captor.getValue().getTextContent()).isEqualTo("Body");

		ModuleContent existing = new ModuleContent(100L, module, ModuleContentType.VIDEO, "Video", null,
				"https://example.test/video", 5);
		when(moduleContentRepository.findTopByModuleIdOrderByPositionDesc(10L)).thenReturn(Optional.of(existing));
		service.createContent(1L, 10L,
				new ModuleContentCreateRequest(ModuleContentType.VIDEO, "Video", null, " https://video.test "));
		verify(moduleContentRepository, org.mockito.Mockito.times(2)).save(captor.capture());
		assertThat(captor.getAllValues().get(2).getPosition()).isEqualTo(6);
	}

	@Test
	void contentUpdatePreservesPosition() {
		ModuleContent content = new ModuleContent(100L, module, ModuleContentType.TEXT, "Text", "Old", null, 4);
		when(courseModuleRepository.findByIdAndCourseId(10L, 1L)).thenReturn(Optional.of(module));
		when(moduleContentRepository.findByIdAndModuleId(100L, 10L)).thenReturn(Optional.of(content));

		var response = service.updateContent(1L, 10L, 100L,
				new ModuleContentUpdateRequest(ModuleContentType.VIDEO, " Video ", null, " url "));

		assertThat(response.type()).isEqualTo(ModuleContentType.VIDEO);
		assertThat(response.videoUrl()).isEqualTo("url");
		assertThat(response.textContent()).isNull();
		assertThat(response.position()).isEqualTo(4);
	}

	@Test
	void moduleDeleteRemovesContentsThenModuleWithoutRenumbering() {
		when(courseModuleRepository.findByIdAndCourseId(10L, 1L)).thenReturn(Optional.of(module));

		service.deleteModule(1L, 10L);

		InOrder order = inOrder(moduleContentRepository, courseModuleRepository);
		order.verify(moduleContentRepository).deleteAllByModuleId(10L);
		order.verify(courseModuleRepository).delete(module);
		verify(courseModuleRepository, never()).save(any());
	}

	@Test
	void moduleWithQuizOrProgressCannotBeDeleted() {
		when(courseModuleRepository.findByIdAndCourseId(10L, 1L)).thenReturn(Optional.of(module));
		when(quizRepository.existsByModuleId(10L)).thenReturn(true);
		assertStatus(HttpStatus.CONFLICT, () -> service.deleteModule(1L, 10L));
		verify(moduleContentRepository, never()).deleteAllByModuleId(10L);

		when(quizRepository.existsByModuleId(10L)).thenReturn(false);
		when(moduleProgressRepository.existsByModuleId(10L)).thenReturn(true);
		assertStatus(HttpStatus.CONFLICT, () -> service.deleteModule(1L, 10L));
	}

	@Test
	void contentDeleteDoesNotRenumberRemainingContent() {
		ModuleContent content = new ModuleContent(100L, module, ModuleContentType.TEXT, "Text", "Body", null, 2);
		when(courseModuleRepository.findByIdAndCourseId(10L, 1L)).thenReturn(Optional.of(module));
		when(moduleContentRepository.findByIdAndModuleId(100L, 10L)).thenReturn(Optional.of(content));

		service.deleteContent(1L, 10L, 100L);

		verify(moduleContentRepository).delete(content);
		verify(moduleContentRepository, never()).save(any());
	}

	@Test
	void wrongCourseModuleAndContentNestingReturnsNotFound() {
		when(courseModuleRepository.findByIdAndCourseId(10L, 2L)).thenReturn(Optional.empty());
		assertStatus(HttpStatus.NOT_FOUND,
				() -> service.updateModule(2L, 10L, new CourseModuleUpdateRequest("Title", null)));

		when(courseModuleRepository.findByIdAndCourseId(10L, 1L)).thenReturn(Optional.of(module));
		when(moduleContentRepository.findByIdAndModuleId(100L, 10L)).thenReturn(Optional.empty());
		assertStatus(HttpStatus.NOT_FOUND, () -> service.updateContent(1L, 10L, 100L,
				new ModuleContentUpdateRequest(ModuleContentType.TEXT, "Title", "Body", null)));
	}

	@Test
	void validatesTextAndVideoContentCombinations() {
		when(courseModuleRepository.findByIdAndCourseId(10L, 1L)).thenReturn(Optional.of(module));

		assertStatus(HttpStatus.BAD_REQUEST, () -> service.createContent(1L, 10L,
				new ModuleContentCreateRequest(ModuleContentType.TEXT, "Text", " ", null)));
		assertStatus(HttpStatus.BAD_REQUEST, () -> service.createContent(1L, 10L,
				new ModuleContentCreateRequest(ModuleContentType.TEXT, "Text", "Body", "video")));
		assertStatus(HttpStatus.BAD_REQUEST, () -> service.createContent(1L, 10L,
				new ModuleContentCreateRequest(ModuleContentType.VIDEO, "Video", null, " ")));
		assertStatus(HttpStatus.BAD_REQUEST, () -> service.createContent(1L, 10L,
				new ModuleContentCreateRequest(ModuleContentType.VIDEO, "Video", "Body", "video")));
	}

	@Test
	void moduleListRequiresExistingCourseAndUsesOrderedRepositoryMethod() {
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(courseModuleRepository.findByCourseIdOrderByPositionAsc(1L)).thenReturn(List.of(module));
		assertThat(service.getModules(1L)).extracting(response -> response.position()).containsExactly(3);
		when(courseRepository.findById(99L)).thenReturn(Optional.empty());
		assertStatus(HttpStatus.NOT_FOUND, () -> service.getModules(99L));
	}

	private void assertStatus(HttpStatus status, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
		assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,
				exception -> assertThat(exception.getStatusCode()).isEqualTo(status));
	}
}
