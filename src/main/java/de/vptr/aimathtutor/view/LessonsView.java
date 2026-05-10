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

import de.vptr.aimathtutor.component.GlassAccentBar;
import de.vptr.aimathtutor.dto.ExerciseViewDto;
import de.vptr.aimathtutor.dto.LessonViewDto;
import de.vptr.aimathtutor.service.ExerciseService;
import de.vptr.aimathtutor.service.LessonService;
import de.vptr.aimathtutor.service.security.AuthService;
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

        AsyncDataLoader.load(() -> {
            final List<LessonViewDto> lessons = this.lessonService.getAllLessons();
            final Map<String, List<ExerciseViewDto>> exercisesByLesson =
                    this.exerciseService.findPublishedExercisesByLessonMap();
            return new LessonsPayload(lessons, exercisesByLesson);
        }, this, this::renderLessons, "Failed to load lessons. Please try again.");
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
        final List<LessonViewDto> rootLessons = payload.lessons.stream().filter(LessonViewDto::isRootLesson).toList();

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
            standaloneSection.setSpacing(false);
            standaloneSection.setPadding(true);
            standaloneSection.setWidthFull();

            final String bgGradient = "linear-gradient(135deg, var(--lumo-base-color),"
                    + " color-mix(in srgb, var(--lumo-base-color) 95%, var(--lumo-primary-color-10pct)))";
            standaloneSection.getStyle().set("background", bgGradient).set("border-radius", "12px")
                    .set("border", "1px solid var(--lumo-contrast-10pct)")
                    .set("box-shadow", "0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)")
                    .set("position", "relative");

            final var accentBar = new GlassAccentBar();

            final var standaloneTitle = new H3("Additional Exercises");
            standaloneTitle.getStyle().set("margin", "0 0 var(--lumo-space-s) 0");

            final var exerciseGrid = new HorizontalLayout();
            exerciseGrid.setSpacing(true);
            exerciseGrid.getStyle().set("flex-wrap", "wrap");

            for (final ExerciseViewDto exercise : standaloneExercises) {
                exerciseGrid.add(this.createExerciseCard(exercise));
            }

            standaloneSection.add(accentBar, standaloneTitle, exerciseGrid);
            this.add(standaloneSection);
        }
    }

    private record LessonsPayload(List<LessonViewDto> lessons, Map<String, List<ExerciseViewDto>> exercisesByLesson) {
    }

    private VerticalLayout createLessonSection(final LessonViewDto lesson, final int depth,
            final Map<String, LessonViewDto> lessonByPublicId,
            final Map<String, List<ExerciseViewDto>> exercisesByLesson, final Set<String> visited) {
        final var section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(depth == 0);
        section.setWidthFull();

        if (depth == 0) {
            final String bgGradient = "linear-gradient(135deg, var(--lumo-base-color),"
                    + " color-mix(in srgb, var(--lumo-base-color) 95%, var(--lumo-primary-color-10pct)))";
            section.getStyle().set("background", bgGradient).set("border-radius", "12px")
                    .set("border", "1px solid var(--lumo-contrast-10pct)")
                    .set("box-shadow", "0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)")
                    .set("position", "relative").set("margin-bottom", "var(--lumo-space-m)");

            section.add(new GlassAccentBar());
        } else {
            section.getStyle().set("border-left", "3px solid var(--lumo-primary-color)").set("padding-left",
                    "var(--lumo-space-m)");
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
            s.getStyle().set("font-weight", "600").set("font-size", "var(--lumo-font-size-s)").set("display", "block");
            section.add(s);
        }

        visited.add(lesson.getPublicId());

        if (lesson.childrenPublicIds != null) {
            for (final String childId : lesson.childrenPublicIds) {
                if (visited.contains(childId)) {
                    continue;
                }
                final LessonViewDto child = lessonByPublicId.get(childId);
                if (child != null) {
                    section.add(
                            this.createLessonSection(child, depth + 1, lessonByPublicId, exercisesByLesson, visited));
                }
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
        } else if (lesson.childrenPublicIds == null || lesson.childrenPublicIds.isEmpty()) {
            final var noExercisesMsg = new Paragraph("No exercises available in this lesson yet.");
            noExercisesMsg.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-style", "italic");
            section.add(noExercisesMsg);
        }

        return section;
    }

    private Div createExerciseCard(final ExerciseViewDto exercise) {
        final var card = new Div();
        final String bgGradient = "linear-gradient(135deg, var(--lumo-base-color),"
                + " color-mix(in srgb, var(--lumo-base-color) 95%, var(--lumo-primary-color-10pct)))";
        card.getStyle().set("width", "300px").set("background", bgGradient)
                .set("border", "1px solid var(--lumo-contrast-10pct)").set("border-radius", "12px")
                .set("cursor", "pointer").set("transition", "all 0.25s cubic-bezier(0.4, 0, 0.2, 1)")
                .set("display", "flex").set("flex-direction", "column").set("position", "relative")
                .set("overflow", "hidden").set("box-shadow", "0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)");

        // Accent bar
        final var accentBar = new GlassAccentBar();

        // Content wrapper
        final var content = new Div();
        content.getStyle().set("padding", "1rem").set("display", "flex").set("flex-direction", "column").set("gap",
                "0.5rem");

        GlassAccentBar.addHoverEffect(card);

        // Title
        final var titleSpan = new Span(exercise.title);
        titleSpan.getStyle().set("font-weight", "600").set("font-size", "var(--lumo-font-size-m)");

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
                case BEGINNER -> difficultyBadge.getElement().getThemeList().add("success");
                case INTERMEDIATE -> difficultyBadge.getElement().getThemeList().add("contrast");
                case ADVANCED, EXPERT -> difficultyBadge.getElement().getThemeList().add("error");
                default -> {
                    // unknown difficulty - no extra styling
                }
            }
            badgeLayout.add(difficultyBadge);
        }

        // Start button
        final var startButton = new Button("Start Exercise");
        startButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        startButton.setWidthFull();
        startButton.addClickListener(ignored -> {
            if (exercise.publicId == null) {
                NotificationUtil.showError("Exercise ID is missing");
                return;
            }
            UI.getCurrent().navigate(ExerciseWorkspaceView.class, new RouteParameters("exerciseId", exercise.publicId));
        });

        content.add(titleSpan, badgeLayout, startButton);
        card.add(accentBar, content);

        return card;
    }
}
