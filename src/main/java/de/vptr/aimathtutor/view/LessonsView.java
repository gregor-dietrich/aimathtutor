package de.vptr.aimathtutor.view;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
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
import de.vptr.aimathtutor.util.AsyncDataLoader;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.inject.Inject;

/**
 * Public lessons view listing available lessons and their exercises.
 */
@Route(value = "", layout = MainLayout.class)
public class LessonsView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    private transient AuthService authService;

    @Inject
    private transient LessonService lessonService;

    @Inject
    private transient ExerciseService exerciseService;

    /**
     * Constructs the LessonsView with alignment and padding.
     */
    public LessonsView() {
        this.setAlignItems(Alignment.START);
        this.setJustifyContentMode(JustifyContentMode.START);
        this.setPadding(true);
        this.setSpacing(true);
        this.setSizeFull();
    }

    /**
     * Prepare the lessons view before navigation by building the UI.
     */
    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        this.buildUi();
    }

    private void buildUi() {
        this.removeAll();

        // Welcome header rendered immediately so the user has feedback while
        // lessons/exercises load in the background.
        final var welcomeLabel = new H2("Welcome, " + this.authService.getUsername() + "!");
        welcomeLabel.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        this.add(welcomeLabel);

        AsyncDataLoader.load(
                () -> {
                    final List<LessonViewDto> lessons = this.lessonService.getAllLessons();
                    final Map<String, List<ExerciseViewDto>> exercisesByLesson = this.exerciseService
                            .findPublishedExercisesByLessonMap();
                    return new LessonsPayload(lessons, exercisesByLesson);
                },
                this,
                this::renderLessons,
                "Failed to load lessons. Please try again.");
    }

    private void renderLessons(final LessonsPayload payload) {
        // Exercises with no lesson are stored under null key
        final List<ExerciseViewDto> standaloneExercises = payload.exercisesByLesson.getOrDefault(null, List.of());

        // Build a lookup map for child lesson resolution
        final Map<String, LessonViewDto> lessonByPublicId = new HashMap<>();
        for (final LessonViewDto l : payload.lessons) {
            lessonByPublicId.put(l.getPublicId(), l);
        }

        // Only root lessons are rendered at the top level
        final List<LessonViewDto> rootLessons = payload.lessons.stream()
                .filter(l -> l.isRootLesson())
                .toList();

        if (rootLessons.isEmpty() && standaloneExercises.isEmpty()) {
            final var noLessonsMsg = new Paragraph("No lessons available yet. Check back soon!");
            noLessonsMsg.getStyle().set("color", "var(--lumo-secondary-text-color)");
            this.add(noLessonsMsg);
            return;
        }

        for (final LessonViewDto lesson : rootLessons) {
            final Set<String> visited = new HashSet<>();
            this.add(this.createLessonSection(lesson, 0, lessonByPublicId, payload.exercisesByLesson, visited));
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

    private record LessonsPayload(List<LessonViewDto> lessons,
            Map<String, List<ExerciseViewDto>> exercisesByLesson) {
    }

    private VerticalLayout createLessonSection(
            final LessonViewDto lesson,
            final int depth,
            final Map<String, LessonViewDto> lessonByPublicId,
            final Map<String, List<ExerciseViewDto>> exercisesByLesson,
            final Set<String> visited) {
        final var section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(depth == 0);
        section.setWidthFull();

        if (depth == 0) {
            section.getStyle()
                    .set("background-color", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "var(--lumo-border-radius-m)")
                    .set("margin-bottom", "var(--lumo-space-m)");
        } else {
            section.getStyle()
                    .set("border-left", "3px solid var(--lumo-primary-color)")
                    .set("padding-left", "var(--lumo-space-m)");
        }

        if (depth == 0) {
            final var h = new H3(lesson.getName());
            h.getStyle().set("margin", "0 0 var(--lumo-space-s) 0");
            section.add(h);
        } else if (depth == 1) {
            final var h = new H4(lesson.getName());
            h.getStyle().set("margin", "0 0 var(--lumo-space-xs) 0");
            section.add(h);
        } else {
            final var s = new Span(lesson.getName());
            s.getStyle()
                    .set("font-weight", "600")
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("display", "block");
            section.add(s);
        }

        visited.add(lesson.getPublicId());

        for (final String childId : lesson.childrenPublicIds) {
            if (visited.contains(childId)) {
                continue;
            }
            final LessonViewDto child = lessonByPublicId.get(childId);
            if (child != null) {
                section.add(this.createLessonSection(child, depth + 1, lessonByPublicId, exercisesByLesson, visited));
            }
        }

        final List<ExerciseViewDto> exercises = exercisesByLesson.getOrDefault(lesson.getPublicId(), List.of());
        if (!exercises.isEmpty()) {
            final var exerciseGrid = new HorizontalLayout();
            exerciseGrid.setSpacing(true);
            exerciseGrid.getStyle().set("flex-wrap", "wrap");
            for (final ExerciseViewDto exercise : exercises) {
                exerciseGrid.add(this.createExerciseCard(exercise));
            }
            section.add(exerciseGrid);
        } else if (lesson.childrenPublicIds.isEmpty()) {
            final var noExercisesMsg = new Paragraph("No exercises available in this lesson yet.");
            noExercisesMsg.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-style", "italic");
            section.add(noExercisesMsg);
        }

        return section;
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
        card.getElement().addEventListener("mouseenter", ignored -> {
            card.getStyle()
                    .set("box-shadow", "0 4px 8px rgba(0,0,0,0.1)")
                    .set("transform", "translateY(-2px)");
        });
        card.getElement().addEventListener("mouseleave", ignored -> {
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
            final var difficultyBadge = new Span(exercise.graspableDifficulty.getValue());
            difficultyBadge.getElement().getThemeList().add("badge");
            switch (exercise.graspableDifficulty) {
                case BEGINNER:
                    difficultyBadge.getElement().getThemeList().add("success");
                    break;
                case INTERMEDIATE:
                    difficultyBadge.getElement().getThemeList().add("contrast");
                    break;
                case ADVANCED:
                case EXPERT:
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
        startButton.addClickListener(ignored -> {
            // Navigate to ExerciseWorkspaceView for Graspable exercises
            // or to a generic ExerciseView for non-Graspable exercises
            if (exercise.publicId == null) {
                NotificationUtil.showError("Exercise ID is missing");
                return;
            }
            UI.getCurrent().navigate(ExerciseWorkspaceView.class,
                    new RouteParameters("exerciseId", exercise.publicId));
        });

        card.add(titleSpan, badgeLayout, startButton);

        return card;
    }
}
