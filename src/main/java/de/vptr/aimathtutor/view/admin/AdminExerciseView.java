package de.vptr.aimathtutor.view.admin;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.component.button.*;
import de.vptr.aimathtutor.component.dialog.FormDialog;
import de.vptr.aimathtutor.component.layout.DateFilterLayout;
import de.vptr.aimathtutor.component.layout.IntegerFilterLayout;
import de.vptr.aimathtutor.component.layout.SearchLayout;
import de.vptr.aimathtutor.dto.ExerciseDto;
import de.vptr.aimathtutor.dto.ExerciseViewDto;
import de.vptr.aimathtutor.dto.LessonViewDto;
import de.vptr.aimathtutor.service.*;
import de.vptr.aimathtutor.util.DateTimeFormatterUtil;
import de.vptr.aimathtutor.util.NotificationUtil;
import de.vptr.aimathtutor.view.LoginView;
import jakarta.inject.Inject;

@Route(value = "admin/exercises", layout = AdminMainLayout.class)
public class AdminExerciseView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger LOG = LoggerFactory.getLogger(AdminExerciseView.class);

    @Inject
    ExerciseService exerciseService;

    @Inject
    LessonService lessonService;

    @Inject
    AuthService authService;

    @Inject
    UserService userService;

    @Inject
    CommentService commentService;

    @Inject
    DateTimeFormatterUtil dateTimeFormatter;

    private Grid<ExerciseViewDto> grid;
    private TextField searchField;
    private Button searchButton;
    private Button showPublishedButton;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private IntegerField userIdField;

    private Dialog exerciseDialog;
    private Binder<ExerciseDto> binder;
    private ExerciseDto currentExercise;
    private List<LessonViewDto> availableLessons;

    public AdminExerciseView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.authService.isAuthenticated()) {
            event.forwardTo(LoginView.class);
            return;
        }

        this.buildUI();
        this.loadLessonsAsync();
        this.loadExercisesAsync();
    }

    private void loadExercisesAsync() {
        LOG.info("Loading exercises");
        try {
            final var exercises = this.exerciseService.getAllExercises();
            LOG.info("Successfully loaded {} exercises", exercises.size());
            this.grid.setItems(exercises);
        } catch (final Exception e) {
            LOG.error("Error loading exercises", e);
            NotificationUtil.showError("Failed to load exercises: " + e.getMessage());
        }
    }

    private void loadPublishedExercisesAsync() {
        LOG.info("Loading published exercises");
        try {
            final var exercises = this.exerciseService.findPublishedExercises();
            LOG.info("Successfully loaded {} published exercises", exercises.size());
            this.grid.setItems(exercises);
        } catch (final Exception e) {
            LOG.error("Error loading published exercises", e);
            NotificationUtil.showError("Failed to load published exercises: " + e.getMessage());
        }
    }

    private void loadLessonsAsync() {
        LOG.info("Loading lessons");
        try {
            this.availableLessons = this.lessonService.getAllLessons();
            LOG.info("Successfully loaded {} lessons", this.availableLessons.size());
        } catch (final Exception e) {
            LOG.error("Error loading lessons", e);
            this.availableLessons = List.of(); // Empty list as fallback
        }
    }

    private void buildUI() {
        this.removeAll();

        final var header = new H2("Exercises");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createGrid();
        this.exerciseDialog = new FormDialog();

        this.add(header, searchLayout, buttonLayout, this.grid);
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(
                e -> {
                    if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                        this.loadExercisesAsync();
                    }
                },
                e -> this.searchExercise(),
                "Search by title or content...",
                "Search Exercises");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        this.showPublishedButton = new Button("Show Published Only", e -> this.loadPublishedExercisesAsync());
        this.showPublishedButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        // Date range filter
        final var dateFilterLayout = new DateFilterLayout(e -> this.filterByDateRange());
        this.startDatePicker = dateFilterLayout.getStartDatePicker();
        this.endDatePicker = dateFilterLayout.getEndDatePicker();

        // User ID filter
        final var userFilterLayout = new IntegerFilterLayout(
                e -> this.filterByUser(),
                "Enter User ID...",
                "Filter by User");
        this.userIdField = userFilterLayout.getIntegerField();

        searchLayout.add(this.showPublishedButton, dateFilterLayout, userFilterLayout);
        return searchLayout;
    }

    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var createButton = new CreateButton(e -> this.openExerciseDialog(null));
        final var refreshButton = new RefreshButton(e -> this.loadExercisesAsync());

        layout.add(createButton, refreshButton);
        return layout;
    }

    private void createGrid() {
        this.grid = new Grid<>(ExerciseViewDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        // Configure columns
        this.grid.addColumn(exercise -> exercise.id).setHeader("ID").setWidth("80px").setFlexGrow(0);

        // Make the title column clickable
        this.grid.addComponentColumn(exercise -> {
            final var titleSpan = new Span(exercise.title);
            titleSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            titleSpan.getStyle().set("cursor", "pointer");
            titleSpan.getStyle().set("width", "100%");
            titleSpan.getStyle().set("display", "block");
            titleSpan.addClickListener(e -> this.openExerciseDialog(exercise));
            return titleSpan;
        }).setHeader("Title").setFlexGrow(2);

        this.grid.addColumn(exercise -> exercise.username != null ? exercise.username : "").setHeader("Author")
                .setWidth("120px")
                .setFlexGrow(0);
        this.grid.addColumn(exercise -> exercise.lessonName != null ? exercise.lessonName : "")
                .setHeader("Lesson")
                .setWidth("120px").setFlexGrow(0);

        this.grid.addComponentColumn(exercise -> {
            final var checkbox = new Checkbox();
            checkbox.setValue(exercise.published != null ? exercise.published : false);
            checkbox.setReadOnly(true);
            return checkbox;
        }).setHeader("Published").setWidth("100px").setFlexGrow(0);

        this.grid.addComponentColumn(exercise -> {
            final var checkbox = new Checkbox();
            checkbox.setValue(exercise.commentable != null ? exercise.commentable : false);
            checkbox.setReadOnly(true);
            return checkbox;
        }).setHeader("Commentable").setWidth("100px").setFlexGrow(0);

        this.grid.addComponentColumn(exercise -> {
            final var checkbox = new Checkbox();
            checkbox.setValue(exercise.graspableEnabled != null ? exercise.graspableEnabled : false);
            checkbox.setReadOnly(true);
            return checkbox;
        }).setHeader("Graspable Math").setWidth("120px").setFlexGrow(0);

        this.grid.addColumn(exercise -> this.dateTimeFormatter.formatDateTime(exercise.created)).setHeader("Created")
                .setWidth("150px").setFlexGrow(0);
        this.grid.addColumn(exercise -> this.dateTimeFormatter.formatDateTime(exercise.lastEdit)).setHeader("Last Edit")
                .setWidth("150px").setFlexGrow(0);

        // Add action column
        this.grid.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("200px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final ExerciseViewDto exercise) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(e -> this.openExerciseDialog(exercise));
        final var deleteButton = new DeleteButton(e -> this.deleteExercise(exercise));
        final var commentButton = new CommentButton(e -> UI.getCurrent().navigate(AdminCommentView.class,
                new QueryParameters(Map.of("exerciseId", List.of(String.valueOf(exercise.id))))));

        layout.add(editButton, deleteButton, commentButton);
        return layout;
    }

    private void openExerciseDialog(final ExerciseViewDto exercise) {
        this.exerciseDialog.removeAll();
        this.currentExercise = exercise != null ? exercise.toExerciseDto() : new ExerciseDto();

        this.binder = new Binder<>(ExerciseDto.class);

        // For new exercises, automatically set the current user as the author
        if (exercise == null) {
            try {
                final var currentUser = this.userService.getCurrentUser();
                this.currentExercise.userId = currentUser.id;
                this.currentExercise.user = new ExerciseDto.UserField();
                this.currentExercise.user.setId(currentUser.id);
                this.currentExercise.user.setUsername(currentUser.username);
            } catch (final Exception e) {
                LOG.error("Error retrieving current user for new exercise", e);
                NotificationUtil.showError("Error retrieving user information. Please try again.");
                return;
            }
        }

        final var title = new H3(exercise != null ? "Edit Exercise" : "Create Exercise");

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // Form fields
        final var titleField = new TextField("Title");
        titleField.setRequired(true);
        titleField.setWidthFull();
        titleField.setInvalid(false); // Clear any previous validation state

        final var contentField = new TextArea("Content");
        contentField.setRequired(true);
        contentField.setWidthFull();
        contentField.setHeight("200px");
        contentField.setInvalid(false); // Clear any previous validation state

        final var publishedField = new Checkbox("Published");
        final var commentableField = new Checkbox("Commentable");

        // Lesson dropdown
        final var lessonField = new ComboBox<LessonViewDto>("Lesson");
        lessonField.setItems(this.availableLessons != null ? this.availableLessons : List.of());
        lessonField.setItemLabelGenerator(LessonViewDto::getName);
        lessonField.setPlaceholder("(none)");
        lessonField.setClearButtonVisible(true);
        lessonField.setInvalid(false); // Clear any previous validation state

        // Bind fields
        this.binder.forField(titleField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Title is required")
                .bind(exercise1 -> exercise1.title, (exercise1, value) -> exercise1.title = value);
        this.binder.forField(contentField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Content is required")
                .bind(exercise1 -> exercise1.content, (exercise1, value) -> exercise1.content = value);
        this.binder.bind(publishedField, exercise1 -> exercise1.published != null ? exercise1.published : false,
                (exercise1, value) -> exercise1.published = value);
        this.binder.bind(commentableField, exercise1 -> exercise1.commentable != null ? exercise1.commentable : false,
                (exercise1, value) -> exercise1.commentable = value);

        // Lesson binding - convert between LessonViewDto and lessonId
        this.binder.bind(lessonField,
                exercise1 -> {
                    if (exercise1.lessonId != null && this.availableLessons != null) {
                        return this.availableLessons.stream()
                                .filter(cat -> cat.getId().equals(exercise1.lessonId))
                                .findFirst()
                                .orElse(null);
                    }
                    return null;
                },
                (exercise1, value) -> {
                    if (value != null) {
                        exercise1.lessonId = value.getId();
                        // Also update the lesson object for consistency
                        if (exercise1.lesson == null) {
                            exercise1.lesson = new ExerciseDto.LessonField();
                        }
                        exercise1.lesson.id = value.getId();
                        exercise1.lesson.name = value.getName();
                    } else {
                        exercise1.lessonId = null;
                        exercise1.lesson = null;
                    }
                });

        // Graspable Math fields
        final var graspableEnabledField = new Checkbox("Enable Graspable Math");
        graspableEnabledField.setTooltipText("Enable interactive math workspace for this exercise");

        final var graspableInitialExpressionField = new TextArea("Initial Expression");
        graspableInitialExpressionField.setPlaceholder("e.g., 2x + 5 = 15");
        graspableInitialExpressionField.setWidthFull();
        graspableInitialExpressionField.setHeight("80px");
        graspableInitialExpressionField.setTooltipText("Starting math expression for the student");

        final var graspableTargetExpressionField = new TextArea("Target Expression (optional)");
        graspableTargetExpressionField.setPlaceholder("e.g., x = 5");
        graspableTargetExpressionField.setWidthFull();
        graspableTargetExpressionField.setHeight("80px");
        graspableTargetExpressionField.setTooltipText("Expected solution to validate against");

        final var graspableDifficultyField = new ComboBox<String>("Difficulty");
        graspableDifficultyField.setItems("beginner", "intermediate", "advanced", "expert");
        graspableDifficultyField.setPlaceholder("Select difficulty");
        graspableDifficultyField.setClearButtonVisible(true);
        graspableDifficultyField.setTooltipText("Problem difficulty level for AI adaptation");

        final var graspableHintsField = new TextArea("Hints (optional)");
        graspableHintsField.setPlaceholder("Enter hints, one per line");
        graspableHintsField.setWidthFull();
        graspableHintsField.setHeight("100px");
        graspableHintsField.setTooltipText("Hints to display when student requests help");

        // Bind Graspable Math fields
        this.binder.bind(graspableEnabledField,
                exercise1 -> exercise1.graspableEnabled != null ? exercise1.graspableEnabled : false,
                (exercise1, value) -> exercise1.graspableEnabled = value);
        this.binder.forField(graspableInitialExpressionField)
                .withValidator((value, ctx) -> {
                    // Only validate if Graspable Math is enabled
                    if (graspableEnabledField.getValue() && (value == null || value.trim().isEmpty())) {
                        return ValidationResult
                                .error("Initial Expression is required when Graspable Math is enabled");
                    }
                    return ValidationResult.ok();
                })
                .bind(exercise1 -> exercise1.graspableInitialExpression,
                        (exercise1, value) -> exercise1.graspableInitialExpression = value);
        this.binder.bind(graspableTargetExpressionField,
                exercise1 -> exercise1.graspableTargetExpression,
                (exercise1, value) -> exercise1.graspableTargetExpression = value);
        this.binder.forField(graspableDifficultyField)
                .withValidator((value, ctx) -> {
                    // Only validate if Graspable Math is enabled
                    if (graspableEnabledField.getValue() && (value == null || value.trim().isEmpty())) {
                        return ValidationResult
                                .error("Difficulty is required when Graspable Math is enabled");
                    }
                    return ValidationResult.ok();
                })
                .bind(exercise1 -> exercise1.graspableDifficulty,
                        (exercise1, value) -> exercise1.graspableDifficulty = value);
        this.binder.bind(graspableHintsField,
                exercise1 -> exercise1.graspableHints,
                (exercise1, value) -> exercise1.graspableHints = value);

        // Show/hide Graspable Math fields based on checkbox
        graspableEnabledField.addValueChangeListener(event -> {
            final boolean enabled = event.getValue();
            graspableInitialExpressionField.setVisible(enabled);
            graspableTargetExpressionField.setVisible(enabled);
            graspableDifficultyField.setVisible(enabled);
            graspableHintsField.setVisible(enabled);
        });

        // Initially hide Graspable Math fields if not enabled
        final boolean initiallyEnabled = this.currentExercise.graspableEnabled != null
                && this.currentExercise.graspableEnabled;
        graspableInitialExpressionField.setVisible(initiallyEnabled);
        graspableTargetExpressionField.setVisible(initiallyEnabled);
        graspableDifficultyField.setVisible(initiallyEnabled);
        graspableHintsField.setVisible(initiallyEnabled);

        form.add(titleField, contentField, lessonField, publishedField, commentableField,
                graspableEnabledField, graspableInitialExpressionField, graspableTargetExpressionField,
                graspableDifficultyField, graspableHintsField);

        // Button layout
        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Save", e -> this.saveExercise());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", e -> this.exerciseDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);
        dialogLayout.setSizeFull();

        this.exerciseDialog.add(dialogLayout);

        // Load current exercise data
        this.binder.readBean(this.currentExercise);

        this.exerciseDialog.open();
    }

    private void saveExercise() {
        try {
            // Validate the form before attempting to save
            if (!this.binder.validate().isOk()) {
                NotificationUtil.showError("Please check the form for errors");
                return;
            }

            this.binder.writeBean(this.currentExercise);

            // Clear timestamp fields to let the backend handle them
            // This prevents issues with timestamp format mismatches
            this.currentExercise.created = null;
            this.currentExercise.lastEdit = null;

            if (this.currentExercise.id == null) {
                this.exerciseService.createExercise(this.currentExercise);
                NotificationUtil.showSuccess("Exercise created successfully");
            } else {
                this.exerciseService.updateExercise(this.currentExercise.id, this.currentExercise);
                NotificationUtil.showSuccess("Exercise updated successfully");
            }

            this.exerciseDialog.close();
            this.loadExercisesAsync();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final Exception e) {
            LOG.error("Unexpected error saving exercise", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void deleteExercise(final ExerciseViewDto exercise) {
        try {
            if (this.exerciseService.deleteExercise(exercise.id)) {
                NotificationUtil.showSuccess("Exercise deleted successfully");
                this.loadExercisesAsync();
            } else {
                NotificationUtil.showError("Failed to delete exercise");
            }
        } catch (final Exception e) {
            LOG.error("Unexpected error deleting exercise", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void searchExercise() {
        final String query = this.searchField.getValue();
        if (query == null || query.trim().isEmpty()) {
            NotificationUtil.showWarning("Please enter a search query");
            return;
        }
        this.searchButton.setEnabled(false);
        this.searchButton.setText("Searching...");
        try {
            final var exercises = this.exerciseService.searchExercises(query.trim());
            this.grid.setItems(exercises);
        } catch (final Exception e) {
            LOG.error("Error searching exercises", e);
            NotificationUtil.showError("Error searching exercises: " + e.getMessage());
        } finally {
            this.searchButton.setEnabled(true);
            this.searchButton.setText("Search");
        }
    }

    private void filterByDateRange() {
        final var startDate = this.startDatePicker.getValue();
        final var endDate = this.endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            NotificationUtil.showWarning("Please select both start and end dates");
            return;
        }

        if (startDate.isAfter(endDate)) {
            NotificationUtil.showWarning("Start date must be before end date");
            return;
        }

        try {
            final var exercises = this.exerciseService.findByDateRange(startDate.toString(), endDate.toString());
            this.grid.setItems(exercises);
        } catch (final Exception e) {
            LOG.error("Error filtering exercises by date range", e);
            NotificationUtil.showError("Error filtering exercises: " + e.getMessage());
        }
    }

    private void filterByUser() {
        final var userId = this.userIdField.getValue();
        if (userId == null) {
            NotificationUtil.showWarning("Please enter a user ID");
            return;
        }

        try {
            final var exercises = this.exerciseService.findByUserId(userId.longValue());
            this.grid.setItems(exercises);
        } catch (final Exception e) {
            LOG.error("Error filtering exercises by user", e);
            NotificationUtil.showError("Error filtering exercises: " + e.getMessage());
        }
    }
}
