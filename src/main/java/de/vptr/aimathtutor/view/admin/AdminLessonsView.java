package de.vptr.aimathtutor.view.admin;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.component.button.CreateButton;
import de.vptr.aimathtutor.component.button.DeleteButton;
import de.vptr.aimathtutor.component.button.EditButton;
import de.vptr.aimathtutor.component.button.RefreshButton;
import de.vptr.aimathtutor.component.dialog.FormDialog;
import de.vptr.aimathtutor.component.layout.SearchLayout;
import de.vptr.aimathtutor.dto.LessonDto;
import de.vptr.aimathtutor.dto.LessonViewDto;
import de.vptr.aimathtutor.entity.LessonEntity;
import de.vptr.aimathtutor.service.AuthService;
import de.vptr.aimathtutor.service.LessonService;
import de.vptr.aimathtutor.util.NotificationUtil;
import de.vptr.aimathtutor.view.LoginView;
import jakarta.inject.Inject;

/**
 * TODO: Class documentation.
 */
@Route(value = "admin/lessons", layout = AdminMainLayout.class)
public class AdminLessonsView extends VerticalLayout implements BeforeEnterObserver {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(AdminLessonsView.class);

    @Inject
    private transient LessonService lessonService;

    @Inject
    private transient AuthService authService;

    private transient TreeGrid<LessonViewDto> treeGrid;
    private transient TextField searchField;
    private transient Button searchButton;
    private transient List<LessonViewDto> allLessons;

    private transient Dialog lessonDialog;
    private transient Binder<LessonDto> binder;
    private transient LessonDto currentLesson;

    public AdminLessonsView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    /**
     * TODO: Document beforeEnter().
     */
    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.authService.isAuthenticated()) {
            event.forwardTo(LoginView.class);
            return;
        }

        this.buildUi();
        this.loadLessonsAsync();
    }

    private void loadLessonsAsync() {
        LOG.info("Loading lessons");

        CompletableFuture.supplyAsync(() -> {
            LOG.info("Loading lessons from service");
            try {
                return this.lessonService.getAllLessons();
            } catch (final Exception e) {
                LOG.error("Error loading lessons", e);
                throw new RuntimeException("Failed to load lessons", e);
            }
        }).orTimeout(30, TimeUnit.SECONDS)
                .whenComplete((lessons, throwable) -> {
                    this.getUI().ifPresent(ui -> ui.access(() -> {
                        if (throwable != null) {
                            LOG.error("Error loading lessons: {}", throwable.getMessage(), throwable);
                            NotificationUtil.showError("Failed to load lessons: " + throwable.getMessage());
                        } else {
                            LOG.info("Successfully loaded {} lessons", lessons.size());
                            this.allLessons = lessons;
                            this.updateTreeGrid();
                        }
                    }));
                });
    }

    private void updateTreeGrid() {
        // Find root lessons (lessons without parent)
        final var rootLessons = this.allLessons.stream()
                .filter(LessonViewDto::isRootLesson)
                .toList();

        this.treeGrid.setItems(rootLessons, this::getChildrenOfLesson);
        this.treeGrid.expandRecursively(rootLessons, 2); // Expand up to 2 levels
    }

    private void updateSearchTreeGrid(final List<LessonViewDto> searchResults) {
        // For search results, we want to show all matching lessons
        // If a matching lesson has a parent that's not in the search results,
        // we show it as a top-level item for better visibility

        // Get all lesson IDs that exist in the search results
        final var lessonIdsInResults = searchResults.stream()
                .map(cat -> cat.id)
                .collect(Collectors.toSet());

        // Find lessons to show at the top level:
        // 1. Root lessons (no parent)
        // 2. Lessons whose parent is not in the search results (orphaned in this
        // context)
        final var topLevelLessons = searchResults.stream()
                .filter(cat -> cat.parentId == null || !lessonIdsInResults.contains(cat.parentId))
                .toList();

        this.treeGrid.setItems(topLevelLessons, lesson -> searchResults.stream()
                .filter(cat -> cat.parentId != null && cat.parentId.equals(lesson.id))
                .toList());

        // Expand all search results for better visibility
        this.treeGrid.expandRecursively(topLevelLessons, 10);
    }

    private List<LessonViewDto> getChildrenOfLesson(final LessonViewDto parent) {
        return this.allLessons.stream()
                .filter(lesson -> lesson.parentId != null && lesson.parentId.equals(parent.id))
                .toList();
    }

    private void buildUi() {
        this.removeAll();

        final var header = new H2("Lessons");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createTreeGrid();
        this.lessonDialog = new FormDialog();

        this.add(header, searchLayout, buttonLayout, this.treeGrid);
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(
                e -> {
                    if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                        this.updateTreeGrid();
                    }
                },
                e -> this.searchLessons(),
                "Search by name...",
                "Search Lessons");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        return searchLayout;
    }

    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var createButton = new CreateButton(e -> this.openLessonDialog(null));
        final var refreshButton = new RefreshButton(e -> this.loadLessonsAsync());

        layout.add(createButton, refreshButton);
        return layout;
    }

    private void createTreeGrid() {
        this.treeGrid = new TreeGrid<>(LessonViewDto.class);
        this.treeGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.treeGrid.setSizeFull();
        this.treeGrid.removeAllColumns();

        // Configure columns
        this.treeGrid.addHierarchyColumn(lesson -> lesson.name)
                .setHeader("Lesson Name")
                .setFlexGrow(3);

        this.treeGrid.addColumn(lesson -> lesson.parentName != null ? lesson.parentName : "N/A")
                .setHeader("Parent")
                .setFlexGrow(2);

        this.treeGrid.addColumn(lesson -> lesson.childrenCount)
                .setHeader("Children")
                .setFlexGrow(0);

        this.treeGrid.addColumn(lesson -> lesson.exercisesCount)
                .setHeader("Lessons")
                .setFlexGrow(0);

        // Add action column
        this.treeGrid.addComponentColumn(this::createActionButtons)
                .setHeader("Actions")
                .setWidth("150px")
                .setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final LessonViewDto lesson) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        // Convert view DTO to a fresh LessonDto and pass that to the dialog
        final var editButton = new EditButton(e -> this.openLessonDialog(lesson.toLessonDto()));
        final var deleteButton = new DeleteButton(e -> this.deleteLesson(lesson.toLessonDto()));

        layout.add(editButton, deleteButton);
        return layout;
    }

    private void openLessonDialog(final LessonDto lesson) {
        this.lessonDialog.removeAll();
        // Use provided LessonDto or start fresh for create
        this.currentLesson = lesson != null ? lesson : new LessonDto();

        this.binder = new Binder<>(LessonDto.class);

        final var title = new H3(lesson != null ? "Edit Lesson" : "Create Lesson");

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // Form fields
        final var nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setInvalid(false); // Clear any previous validation state

        final var parentField = new ComboBox<LessonDto>("Parent Lesson");
        parentField.setItemLabelGenerator(cat -> cat != null ? cat.name : "N/A");
        parentField.setPlaceholder("(none)");
        parentField.setInvalid(false); // Clear any previous validation state
        if (this.allLessons != null) {
            // Only show lessons that are not descendants of the current lesson
            final var availableParents = this.allLessons.stream()
                    .map(LessonViewDto::toLessonDto)
                    .filter(cat -> lesson == null || !this.isDescendantOf(cat, lesson))
                    .filter(cat -> lesson == null || !cat.id.equals(lesson.id))
                    .toList();
            parentField.setItems(availableParents);
        }
        // Allow clearing the selection to make it a root lesson
        parentField.setClearButtonVisible(true);

        // Bind fields
        this.binder.forField(nameField)
                .withValidator(name -> name != null && !name.trim().isEmpty(), "Name is required")
                .bind(cat -> cat.name, (cat, value) -> cat.name = value);
        this.binder.forField(parentField)
                .bind(
                        cat -> {
                            // Convert from DTO parent field
                            if (cat.parent != null && cat.parent.id != null) {
                                // Find the LessonDto from available parents
                                if (this.allLessons != null) {
                                    return this.allLessons.stream()
                                            .map(LessonViewDto::toLessonDto)
                                            .filter(c -> c.id.equals(cat.parent.id))
                                            .findFirst()
                                            .orElse(null);
                                }
                            }
                            return null;
                        },
                        (cat, value) -> {
                            // Convert back to DTO parent field
                            if (value != null && value.id != null) {
                                cat.parentId = value.id;
                                if (cat.parent == null) {
                                    cat.parent = new LessonDto.ParentField();
                                }
                                cat.parent.id = value.id;
                            } else {
                                cat.parentId = null;
                                cat.parent = null;
                            }
                        });

        form.add(nameField, parentField);

        // Button layout
        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Save", e -> this.saveLesson());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", e -> this.lessonDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);
        dialogLayout.setSizeFull();

        this.lessonDialog.add(dialogLayout);

        // Load current lesson data
        this.binder.readBean(this.currentLesson);

        this.lessonDialog.open();
    }

    private boolean isDescendantOf(final LessonDto potential, final LessonDto ancestor) {
        if (potential.parent == null) {
            return false;
        }
        if (potential.parent.id.equals(ancestor.id)) {
            return true;
        }

        // Find the parent in the list and check recursively
        final var parent = this.allLessons.stream()
                .filter(cat -> cat.id.equals(potential.parent.id))
                .map(LessonViewDto::toLessonDto)
                .findFirst()
                .orElse(null);

        if (parent != null) {
            return this.isDescendantOf(parent, ancestor);
        }

        return false;
    }

    private void saveLesson() {
        try {
            this.binder.writeBean(this.currentLesson);

            // Sync parent field
            this.currentLesson.syncParent();

            // Convert DTO to Entity for service call
            final var lessonEntity = new LessonEntity();
            lessonEntity.id = this.currentLesson.id;
            lessonEntity.name = this.currentLesson.name;

            // Set parent if specified
            if (this.currentLesson.parentId != null) {
                final var parentEntity = new LessonEntity();
                parentEntity.id = this.currentLesson.parentId;
                lessonEntity.parent = parentEntity;
            }

            if (this.currentLesson.id == null) {
                this.lessonService.createLesson(lessonEntity);
                NotificationUtil.showSuccess("Lesson created successfully");
            } else {
                this.lessonService.updateLesson(lessonEntity);
                NotificationUtil.showSuccess("Lesson updated successfully");
            }

            this.lessonDialog.close();
            this.loadLessonsAsync();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final Exception e) {
            LOG.error("Unexpected error saving lesson", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void deleteLesson(final LessonDto lesson) {
        // Check if lesson has children
        final boolean hasChildren = this.allLessons.stream()
                .anyMatch(cat -> cat.parentId != null && cat.parentId.equals(lesson.id));

        if (hasChildren) {
            NotificationUtil
                    .showError("Cannot delete lesson with sub-lessons. Please delete or move sub-lessons first.");
            return;
        }

        try {
            if (this.lessonService.deleteLesson(lesson.id)) {
                NotificationUtil.showSuccess("Lesson deleted successfully");
                this.loadLessonsAsync();
            } else {
                NotificationUtil.showError("Failed to delete lesson");
            }
        } catch (final Exception e) {
            LOG.error("Unexpected error deleting lesson", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void searchLessons() {
        final String query = this.searchField.getValue();
        if (query == null || query.trim().isEmpty()) {
            // If query is empty, return to normal view
            this.updateTreeGrid();
            return;
        }

        this.searchButton.setEnabled(false);
        this.searchButton.setText("Searching...");
        CompletableFuture.supplyAsync(() -> {
            try {
                return this.lessonService.searchLessons(query.trim());
            } catch (final Exception e) {
                LOG.error("Unexpected error searching lessons", e);
                throw new RuntimeException("Unexpected error occurred", e);
            }
        }).orTimeout(30, TimeUnit.SECONDS)
                .whenComplete((lessons, throwable) -> {
                    this.getUI().ifPresent(ui -> ui.access(() -> {
                        this.searchButton.setEnabled(true);
                        this.searchButton.setText("Search");
                        if (throwable != null) {
                            LOG.error("Error searching lessons: {}", throwable.getMessage(), throwable);
                            NotificationUtil.showError(throwable.getCause() != null ? throwable.getCause().getMessage()
                                    : throwable.getMessage());
                        } else {
                            // Store search results and update the tree grid
                            this.updateSearchTreeGrid(lessons);
                        }
                    }));
                });
    }
}
