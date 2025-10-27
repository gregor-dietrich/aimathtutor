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

/**
 * TODO: Class documentation.
 */
@Route(value = "", layout = MainLayout.class)
public class LessonsView extends VerticalLayout implements BeforeEnterObserver {

    private static final long serialVersionUID = 1L;

    @Inject
    private transient AuthService authService;

    @Inject
    private transient LessonService lessonService;

    @Inject
    private transient ExerciseService exerciseService;

    public LessonsView() {
        this.setAlignItems(Alignment.START);
        this.setJustifyContentMode(JustifyContentMode.START);
        this.setPadding(true);
        this.setSpacing(true);
        this.setSizeFull();
    }

    /**
     * TODO: Document beforeEnter().
     */
    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        this.buildUi();
    }

    private void buildUi() {
        this.removeAll();

        // Welcome header
        final var welcomeLabel = new H2("Welcome, " + this.authService.getUsername() + "!");
        welcomeLabel.getStyle().set("margin-bottom", "var(--lumo-space-m)");

        this.add(welcomeLabel);

        // Get all lessons
        final List<LessonViewDto> lessons = this.lessonService.getAllLessons();

        // Also show standalone exercises (not in any lesson)
        final List<ExerciseViewDto> standaloneExercises = this.exerciseService.findPublishedExercises().stream()
                .filter(ex -> ex.lessonId == null)
                .toList();

        if (lessons.isEmpty() && standaloneExercises.isEmpty()) {
            final var noLessonsMsg = new Paragraph("No lessons available yet. Check back soon!");
            noLessonsMsg.getStyle().set("color", "var(--lumo-secondary-text-color)");
            this.add(noLessonsMsg);
            return;
        }

        // Display each lesson with its exercises
        for (final LessonViewDto lesson : lessons) {
            if (lesson.exercisesCount > 0) {
                this.add(this.createLessonCard(lesson));
            }
        }

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
                exerciseGrid.add(this.createExerciseCard(exercise));
            }

            standaloneSection.add(exerciseGrid);
            this.add(standaloneSection);
        }
    }

    private VerticalLayout createLessonCard(final LessonViewDto lesson) {
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
                exerciseGrid.add(this.createExerciseCard(exercise));
            }

            lessonCard.add(exerciseGrid);
        }

        return lessonCard;
    }

    private Div createExerciseCard(final ExerciseViewDto exercise) {
        final var card = new Div();
        card.getStyle()
                .set("width", "300px")
                .set("padding", "1rem")
                .set("background-color", "var(--lumo-contrast-5pct)")
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
                .set("font-size", "var(--lumo-font-size-m)");

        // Badges
        final var badgeLayout = new HorizontalLayout();
        badgeLayout.setSpacing(true);
        badgeLayout.getStyle().set("flex-wrap", "wrap");

        // Completed badge
        if (Boolean.TRUE.equals(exercise.userCompleted)) {
            final var completedBadge = new Span("✓ Completed");
            completedBadge.getElement().getThemeList().add("badge");
            completedBadge.getElement().getThemeList().add("success");
            badgeLayout.add(completedBadge);
        }

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
                default:
                    // unknown difficulty - no extra styling
                    break;
            }
            badgeLayout.add(difficultyBadge);
        }

        // Start button
        final var startButton = new Button("Start Exercise");
        startButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        startButton.setWidthFull();
        startButton.addClickListener(e -> {
            // Navigate to ExerciseWorkspaceView for Graspable exercises
            // or to a generic ExerciseView for non-Graspable exercises
            UI.getCurrent().navigate(ExerciseWorkspaceView.class,
                    new RouteParameters("exerciseId", exercise.id.toString()));
        });

        card.add(titleSpan, badgeLayout, startButton);

        return card;
    }
}
