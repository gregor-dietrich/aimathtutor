package de.vptr.aimathtutor.view.admin;

import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

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
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.component.button.CommentButton;
import de.vptr.aimathtutor.component.button.CreateButton;
import de.vptr.aimathtutor.component.button.DeleteButton;
import de.vptr.aimathtutor.component.button.EditButton;
import de.vptr.aimathtutor.component.button.RefreshButton;
import de.vptr.aimathtutor.component.dialog.FormDialog;
import de.vptr.aimathtutor.component.layout.DateFilterLayout;
import de.vptr.aimathtutor.component.layout.IntegerFilterLayout;
import de.vptr.aimathtutor.component.layout.SearchLayout;
import de.vptr.aimathtutor.dto.ExerciseDto;
import de.vptr.aimathtutor.dto.ExerciseDto.DifficultyLevel;
import de.vptr.aimathtutor.dto.ExerciseViewDto;
import de.vptr.aimathtutor.dto.LessonViewDto;
import de.vptr.aimathtutor.exception.PermissionDeniedException;
import de.vptr.aimathtutor.service.ExerciseService;
import de.vptr.aimathtutor.service.LessonService;
import de.vptr.aimathtutor.service.UserService;
import de.vptr.aimathtutor.util.AppConstants;
import de.vptr.aimathtutor.util.AsyncDataLoader;
import de.vptr.aimathtutor.util.DateTimeFormatterUtil;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.inject.Inject;

/**
 * Admin view for managing exercises: listing, editing, and publishing.
 */
@Route(value = "admin/exercises", layout = AdminMainLayout.class)
public class AdminExercisesView extends AbstractAdminView {

    private static final Logger LOG = Logger.getLogger(AdminExercisesView.class);

    @Inject
    private transient ExerciseService exerciseService;

    @Inject
    private transient LessonService lessonService;
    @Inject
    private transient UserService userService;

    @Inject
    private transient DateTimeFormatterUtil dateTimeFormatter;

    private Grid<ExerciseViewDto> grid;
    private TextField searchField;
    private Button searchButton;
    private Button showPublishedButton;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private IntegerField userIdField;

    private transient Dialog exerciseDialog;
    private transient Binder<ExerciseDto> binder;
    private transient ExerciseDto currentExercise;
    private transient List<LessonViewDto> availableLessons;

    /**
     * Constructs the AdminExercisesView with full size and padding.
     */
    public AdminExercisesView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    /**
     * Verify authentication and set up the exercise management UI before the
     * view is displayed.
     */
    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.isAuthOk(event)) {
            return;
        }

        this.buildUi();
        this.loadLessons();
        this.loadExercises();
    }

    private void loadExercises() {
        LOG.info("Loading exercises");
        AsyncDataLoader.load(
                () -> this.exerciseService.getAllExercises(),
                this,
                exercises -> {
                    LOG.infof("Successfully loaded %s exercises",  exercises.size());
                    this.grid.setItems(exercises);
                },
                "Failed to load exercises. Please try again.");
    }

    private void loadPublishedExercises() {
        LOG.info("Loading published exercises");
        AsyncDataLoader.load(
                () -> this.exerciseService.findPublishedExercises(),
                this,
                exercises -> {
                    LOG.infof("Successfully loaded %s published exercises",  exercises.size());
                    this.grid.setItems(exercises);
                },
                "Failed to load published exercises. Please try again.");
    }

    private void loadLessons() {
        LOG.info("Loading lessons");
        AsyncDataLoader.load(
                () -> this.lessonService.getAllLessons(),
                this,
                lessons -> {
                    this.availableLessons = lessons;
                    LOG.infof("Successfully loaded %s lessons",  this.availableLessons.size());
                },
                () -> {
                    this.availableLessons = List.of();
                },
                "Failed to load lessons. Please try again.");
    }

    /**
     * Build the UI for exercise management, including header, search and grid.
     */
    private void buildUi() {
        this.removeAll();

        final var header = new H2("Exercises");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createGrid();
        this.exerciseDialog = new FormDialog();

        this.add(header, searchLayout, buttonLayout, this.grid);
    }

    /**
     * Create the search layout for filtering exercises by text, date and user.
     *
     * @return the search layout
     */
    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(
                e -> {
                    if (e.getValue() == null || e.getValue().isBlank()) {
                        this.loadExercises();
                    }
                },
                ignored -> this.searchExercise(),
                "Search by title or content...",
                "Search Exercises");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        this.showPublishedButton = new Button("Show Published Only", ignored -> this.loadPublishedExercises());
        this.showPublishedButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        // Date range filter
        final var dateFilterLayout = new DateFilterLayout(ignored -> this.filterByDateRange());
        this.startDatePicker = dateFilterLayout.getStartDatePicker();
        this.endDatePicker = dateFilterLayout.getEndDatePicker();

        // User ID filter
        final var userFilterLayout = new IntegerFilterLayout(
                ignored -> this.filterByUser(),
                "Enter User ID...",
                "Filter by User");
        this.userIdField = userFilterLayout.getIntegerField();

        searchLayout.add(this.showPublishedButton, dateFilterLayout, userFilterLayout);
        return searchLayout;
    }

    /**
     * Create the layout holding action buttons for exercises.
     *
     * @return a horizontal layout with action buttons
     */
    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var createButton = new CreateButton(ignored -> this.openExerciseDialog(null));
        final var refreshButton = new RefreshButton(ignored -> this.loadExercises());

        layout.add(createButton, refreshButton);
        return layout;
    }

    private void createGrid() {
        this.grid = new Grid<>(ExerciseViewDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        // Configure columns
        this.grid.addColumn(exercise -> exercise.publicId).setHeader("ID").setWidth(AppConstants.GRID_ID_WIDTH)
                .setFlexGrow(0);

        // Make the title column clickable
        this.grid.addComponentColumn(exercise -> {
            final var titleSpan = new Span(exercise.title);
            titleSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            titleSpan.getStyle().set("cursor", "pointer");
            titleSpan.getStyle().set("width", "100%");
            titleSpan.getStyle().set("display", "block");
            titleSpan.addClickListener(ignored -> this.openExerciseDialog(exercise));
            return titleSpan;
        }).setHeader("Title").setFlexGrow(2);

        this.grid.addColumn(exercise -> exercise.username != null ? exercise.username : "").setHeader("Author")
                .setWidth("120px")
                .setFlexGrow(0);
        this.grid.addColumn(exercise -> exercise.lessonName != null ? exercise.lessonName : "")
                .setHeader("Lesson")
                .setFlexGrow(1);

        this.grid.addComponentColumn(exercise -> {
            final var checkbox = new Checkbox();
            checkbox.setValue(exercise.published);
            checkbox.setReadOnly(true);
            return checkbox;
        }).setHeader("Published").setWidth("100px").setFlexGrow(0);

        this.grid.addComponentColumn(exercise -> {
            final var checkbox = new Checkbox();
            checkbox.setValue(exercise.commentable);
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
                .setWidth("180px").setFlexGrow(0);
        this.grid.addColumn(exercise -> this.dateTimeFormatter.formatDateTime(exercise.lastEdit)).setHeader("Last Edit")
                .setWidth("180px").setFlexGrow(0);

        // Add action column
        this.grid.addComponentColumn(this::createActionButtons).setHeader("Actions")
                .setWidth(AppConstants.GRID_NAME_WIDTH).setFlexGrow(0);
    }

    /**
     * Create action buttons (edit, delete, comment) for an exercise row.
     *
     * @param exercise the exercise view dto
     * @return a horizontal layout with action buttons
     */
    private HorizontalLayout createActionButtons(final ExerciseViewDto exercise) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(ignored -> this.openExerciseDialog(exercise));
        final var deleteButton = new DeleteButton(ignored -> this.deleteExercise(exercise));
        final var commentButton = new CommentButton(ignored -> UI.getCurrent().navigate(AdminCommentsView.class,
                new QueryParameters(Map.of("exerciseId", List.of(exercise.publicId)))));

        layout.add(editButton, deleteButton, commentButton);
        return layout;
    }

    /**
     * Open a dialog to edit or create an exercise.
     *
     * @param exercise the exercise to edit or null to create a new one
     */
    private void openExerciseDialog(final ExerciseViewDto exercise) {
        this.exerciseDialog.removeAll();
        this.currentExercise = exercise != null ? exercise.toExerciseDto() : new ExerciseDto();

        this.binder = new Binder<>(ExerciseDto.class);

        // For new exercises, automatically set the current user as the author
        if (exercise == null) {
            try {
                final var currentUser = this.userService.getCurrentUser();
                if (currentUser == null || currentUser.publicId == null) {
                    NotificationUtil.showError("Error retrieving user information. Please try again.");
                    return;
                }
                this.currentExercise.userPublicId = currentUser.publicId;
                this.currentExercise.user = new ExerciseDto.UserField();
                this.currentExercise.user.setPublicId(currentUser.publicId);
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
                .withValidator(value -> value != null && !value.isBlank(), "Title is required")
                .bind(exercise1 -> exercise1.title, (exercise1, value) -> exercise1.title = value);
        this.binder.forField(contentField)
                .withValidator(value -> value != null && !value.isBlank(), "Content is required")
                .bind(exercise1 -> exercise1.content, (exercise1, value) -> exercise1.content = value);
        this.binder.bind(publishedField, exercise1 -> exercise1.published,
                (exercise1, value) -> exercise1.published = value);
        this.binder.bind(commentableField, exercise1 -> exercise1.commentable,
                (exercise1, value) -> exercise1.commentable = value);

        // Lesson binding - convert between LessonViewDto and lessonPublicId
        this.binder.bind(lessonField,
                exercise1 -> {
                    if (exercise1.lessonPublicId != null && this.availableLessons != null) {
                        return this.availableLessons.stream()
                                .filter(cat -> cat.getPublicId().equals(exercise1.lessonPublicId))
                                .findFirst()
                                .orElse(null);
                    }
                    return null;
                },
                (exercise1, value) -> {
                    if (value != null) {
                        exercise1.lessonPublicId = value.getPublicId();
                        // Also update the lesson object for consistency
                        if (exercise1.lesson == null) {
                            exercise1.lesson = new ExerciseDto.LessonField();
                        }
                        exercise1.lesson.publicId = value.getPublicId();
                        exercise1.lesson.name = value.getName();
                    } else {
                        exercise1.lessonPublicId = null;
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

        final var graspableTargetExpressionField = new TextArea("Target Expression");
        graspableTargetExpressionField.setPlaceholder("e.g., x = 5");
        graspableTargetExpressionField.setWidthFull();
        graspableTargetExpressionField.setHeight("80px");
        graspableTargetExpressionField.setTooltipText("Expected solution to validate against");

        final var graspableDifficultyField = new ComboBox<DifficultyLevel>("Difficulty");
        graspableDifficultyField.setItems(DifficultyLevel.values());
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
                .withValidator((value, ignored) -> {
                    // Only validate if Graspable Math is enabled
                    if (graspableEnabledField.getValue() && (value == null || value.isBlank())) {
                        return ValidationResult.error("Initial Expression is required when Graspable Math is enabled");
                    }
                    return ValidationResult.ok();
                })
                .bind(exercise1 -> exercise1.graspableInitialExpression,
                        (exercise1, value) -> exercise1.graspableInitialExpression = value);
        this.binder.forField(graspableTargetExpressionField)
                .withValidator((value, ignored) -> {
                    // Only validate if Graspable Math is enabled
                    if (graspableEnabledField.getValue() && (value == null || value.isBlank())) {
                        return ValidationResult.error("Target Expression is required when Graspable Math is enabled");
                    }
                    return ValidationResult.ok();
                })
                .bind(exercise1 -> exercise1.graspableTargetExpression,
                        (exercise1, value) -> exercise1.graspableTargetExpression = value);
        this.binder.forField(graspableDifficultyField)
                .withValidator((value, ignored) -> {
                    // Only validate if Graspable Math is enabled
                    if (graspableEnabledField.getValue() && value == null) {
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

        final var saveButton = new Button("Save", ignored -> this.saveExercise());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", ignored -> this.exerciseDialog.close());
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

    /**
     * Save the current exercise being edited in the dialog.
     */
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

            if (this.currentExercise.publicId == null) {
                this.exerciseService.createExercise(this.currentExercise);
                NotificationUtil.showSuccess("Exercise created successfully");
            } else {
                this.exerciseService.updateExercise(this.currentExercise.publicId, this.currentExercise);
                NotificationUtil.showSuccess("Exercise updated successfully");
            }

            this.exerciseDialog.close();
            // Refresh exercises and lessons so computed columns (exercise counts) update
            this.loadExercises();
            this.loadLessons();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final PermissionDeniedException e) {
            NotificationUtil.showError(e.getMessage());
        } catch (final Exception e) {
            LOG.error("Unexpected error saving exercise", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    /**
     * Delete the provided exercise.
     *
     * @param exercise the exercise to delete
     */
    private void deleteExercise(final ExerciseViewDto exercise) {
        try {
            if (this.exerciseService.deleteExercise(exercise.publicId)) {
                NotificationUtil.showSuccess("Exercise deleted successfully");
                this.loadExercises();
                this.loadLessons();
            } else {
                NotificationUtil.showError("Failed to delete exercise");
            }
        } catch (final PermissionDeniedException e) {
            NotificationUtil.showError(e.getMessage());
        } catch (final Exception e) {
            LOG.error("Unexpected error deleting exercise", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    /**
     * Search for exercises using the current search query and update the grid.
     */
    private void searchExercise() {
        final String query = this.searchField.getValue();
        if (query == null || query.isBlank()) {
            NotificationUtil.showWarning("Please enter a search query");
            return;
        }
        this.searchButton.setEnabled(false);
        this.searchButton.setText("Searching...");
        AsyncDataLoader.load(
                () -> this.exerciseService.searchExercises(query.trim()),
                this,
                exercises -> {
                    this.grid.setItems(exercises);
                    this.searchButton.setEnabled(true);
                    this.searchButton.setText("Search");
                },
                () -> {
                    this.searchButton.setEnabled(true);
                    this.searchButton.setText("Search");
                },
                "An error occurred while searching exercises. Please try again.");
    }

    /**
     * Filter the exercises by the selected date range.
     */
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

        AsyncDataLoader.load(
                () -> this.exerciseService.findByDateRange(startDate.toString(), endDate.toString()),
                this,
                exercises -> this.grid.setItems(exercises),
                "An error occurred while filtering exercises. Please try again.");
    }

    private void filterByUser() {
        final var userId = this.userIdField.getValue();
        if (userId == null) {
            NotificationUtil.showWarning("Please enter a user ID");
            return;
        }

        AsyncDataLoader.load(
                () -> this.exerciseService.findByUserId(userId.longValue()),
                this,
                exercises -> this.grid.setItems(exercises),
                "An error occurred while filtering exercises. Please try again.");
    }
}
