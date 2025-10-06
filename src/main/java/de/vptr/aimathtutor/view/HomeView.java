package de.vptr.aimathtutor.view;

import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;

import de.vptr.aimathtutor.dto.ExerciseViewDto;
import de.vptr.aimathtutor.dto.LessonViewDto;
import de.vptr.aimathtutor.service.AuthService;
import de.vptr.aimathtutor.service.ExerciseService;
import de.vptr.aimathtutor.service.LessonService;
import jakarta.inject.Inject;

@Route(value = "", layout = MainLayout.class)
public class HomeView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    AuthService authService;

    @Inject
    LessonService lessonService;

    @Inject
    ExerciseService exerciseService;

    public HomeView() {
        this.setAlignItems(Alignment.START);
        this.setJustifyContentMode(JustifyContentMode.START);
        this.setPadding(true);
        this.setSpacing(true);
        this.setSizeFull();
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        this.buildUI();
    }

    private void buildUI() {
        this.removeAll();

        // Welcome header
        final var welcomeLabel = new H2("Welcome, " + this.authService.getUsername() + "!");
        welcomeLabel.getStyle().set("margin-bottom", "var(--lumo-space-m)");

        this.add(welcomeLabel);

        // Get all lessons
        final List<LessonViewDto> lessons = this.lessonService.getAllLessons();

        if (lessons.isEmpty()) {
            final var noLessonsMsg = new Paragraph("No lessons available yet. Check back soon!");
            noLessonsMsg.getStyle().set("color", "var(--lumo-secondary-text-color)");
            this.add(noLessonsMsg);
            return;
        }

        // Display each lesson with its exercises
        for (final LessonViewDto lesson : lessons) {
            this.add(createLessonCard(lesson));
        }

        // Also show standalone exercises (not in any lesson)
        final List<ExerciseViewDto> standaloneExercises = this.exerciseService.findPublishedExercises().stream()
                .filter(ex -> ex.lessonId == null)
                .toList();

        if (!standaloneExercises.isEmpty()) {
            final var standaloneSection = new VerticalLayout();
            standaloneSection.setSpacing(true);
            standaloneSection.setPadding(false);
            standaloneSection.setWidthFull();

            final var standaloneTitle = new H3("Additional Exercises");
            standaloneSection.add(standaloneTitle);

            final var exerciseGrid = new HorizontalLayout();
            exerciseGrid.setSpacing(true);
            exerciseGrid.getStyle().set("flex-wrap", "wrap");

            for (final ExerciseViewDto exercise : standaloneExercises) {
                exerciseGrid.add(createExerciseCard(exercise));
            }

            standaloneSection.add(exerciseGrid);
            this.add(standaloneSection);
        }
    }

    private VerticalLayout createLessonCard(LessonViewDto lesson) {
        final var lessonCard = new VerticalLayout();
        lessonCard.setSpacing(true);
        lessonCard.setPadding(true);
        lessonCard.setWidthFull();
        lessonCard.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("margin-bottom", "var(--lumo-space-m)");

        // Lesson title and description
        final var lessonTitle = new H3(lesson.getName());
        lessonTitle.getStyle().set("margin", "0");
        lessonCard.add(lessonTitle);

        // Get exercises for this lesson
        final List<ExerciseViewDto> exercises = this.exerciseService.findByLessonId(lesson.getId()).stream()
                .filter(ex -> Boolean.TRUE.equals(ex.published))
                .toList();

        if (exercises.isEmpty()) {
            final var noExercisesMsg = new Paragraph("No exercises available in this lesson yet.");
            noExercisesMsg.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-style", "italic");
            lessonCard.add(noExercisesMsg);
        } else {
            // Exercise cards in a horizontal layout
            final var exerciseGrid = new HorizontalLayout();
            exerciseGrid.setSpacing(true);
            exerciseGrid.getStyle().set("flex-wrap", "wrap");

            for (final ExerciseViewDto exercise : exercises) {
                exerciseGrid.add(createExerciseCard(exercise));
            }

            lessonCard.add(exerciseGrid);
        }

        return lessonCard;
    }

    private Div createExerciseCard(ExerciseViewDto exercise) {
        final var card = new Div();
        card.getStyle()
                .set("width", "300px")
                .set("padding", "1rem")
                .set("background-color", "white")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("cursor", "pointer")
                .set("transition", "all 0.2s ease")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "0.5rem");

        // Hover effect
        card.getElement().addEventListener("mouseenter", e -> {
            card.getStyle()
                    .set("box-shadow", "0 4px 8px rgba(0,0,0,0.1)")
                    .set("transform", "translateY(-2px)");
        });
        card.getElement().addEventListener("mouseleave", e -> {
            card.getStyle()
                    .set("box-shadow", "none")
                    .set("transform", "translateY(0)");
        });

        // Title
        final var titleSpan = new Span(exercise.title);
        titleSpan.getStyle()
                .set("font-weight", "600")
                .set("font-size", "var(--lumo-font-size-m)")
                .set("color", "var(--lumo-primary-text-color)");

        // Badges
        final var badgeLayout = new HorizontalLayout();
        badgeLayout.setSpacing(true);
        badgeLayout.getStyle().set("flex-wrap", "wrap");

        // Graspable Math badge
        if (Boolean.TRUE.equals(exercise.graspableEnabled)) {
            final var gmBadge = new Span("📐 Interactive");
            gmBadge.getElement().getThemeList().add("badge");
            gmBadge.getElement().getThemeList().add("success");
            badgeLayout.add(gmBadge);
        }

        // Difficulty badge
        if (exercise.graspableDifficulty != null) {
            final var difficultyBadge = new Span(exercise.graspableDifficulty);
            difficultyBadge.getElement().getThemeList().add("badge");
            switch (exercise.graspableDifficulty.toLowerCase()) {
                case "beginner":
                    difficultyBadge.getElement().getThemeList().add("success");
                    break;
                case "intermediate":
                    difficultyBadge.getElement().getThemeList().add("contrast");
                    break;
                case "advanced":
                case "expert":
                    difficultyBadge.getElement().getThemeList().add("error");
                    break;
            }
            badgeLayout.add(difficultyBadge);
        }

        // Content preview
        final var contentPreview = new Paragraph();
        if (exercise.content != null && !exercise.content.trim().isEmpty()) {
            String preview = exercise.content.replaceAll("<[^>]*>", ""); // Strip HTML tags
            if (preview.length() > 100) {
                preview = preview.substring(0, 100) + "...";
            }
            contentPreview.setText(preview);
        } else {
            contentPreview.setText("Click to start working on this exercise");
        }
        contentPreview.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("margin", "0");

        // Start button
        final var startButton = new Button("Start Exercise");
        startButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        startButton.setWidthFull();
        startButton.addClickListener(e -> {
            if (Boolean.TRUE.equals(exercise.graspableEnabled)) {
                // Navigate to ExerciseWorkspaceView
                UI.getCurrent().navigate(ExerciseWorkspaceView.class,
                        new RouteParameters("exerciseId", exercise.id.toString()));
            } else {
                // For non-Graspable Math exercises, could navigate to a different view
                // For now, show a notification
                com.vaadin.flow.component.notification.Notification.show(
                        "This exercise doesn't have interactive workspace yet",
                        3000,
                        com.vaadin.flow.component.notification.Notification.Position.MIDDLE);
            }
        });

        card.add(titleSpan, badgeLayout, contentPreview, startButton);

        return card;
    }
}