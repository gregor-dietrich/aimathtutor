package de.vptr.aimathtutor.view.admin;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
import de.vptr.aimathtutor.rest.dto.PostCategoryDto;
import de.vptr.aimathtutor.rest.dto.PostCategoryViewDto;
import de.vptr.aimathtutor.rest.entity.PostCategoryEntity;
import de.vptr.aimathtutor.rest.service.AuthService;
import de.vptr.aimathtutor.rest.service.PostCategoryService;
import de.vptr.aimathtutor.util.NotificationUtil;
import de.vptr.aimathtutor.view.LoginView;
import jakarta.inject.Inject;

@Route(value = "admin/categories", layout = AdminMainLayout.class)
public class AdminPostCategoryView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger LOG = LoggerFactory.getLogger(AdminPostCategoryView.class);

    @Inject
    PostCategoryService categoryService;

    @Inject
    AuthService authService;

    private TreeGrid<PostCategoryViewDto> treeGrid;
    private TextField searchField;
    private Button searchButton;
    private List<PostCategoryViewDto> allCategories;

    private Dialog categoryDialog;
    private Binder<PostCategoryDto> binder;
    private PostCategoryDto currentCategory;

    public AdminPostCategoryView() {
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
        this.loadCategoriesAsync();
    }

    private void loadCategoriesAsync() {
        LOG.info("Loading categories");

        CompletableFuture.supplyAsync(() -> {
            LOG.info("Loading categories from service");
            try {
                return this.categoryService.getAllCategories();
            } catch (final Exception e) {
                LOG.error("Error loading categories", e);
                throw new RuntimeException("Failed to load categories", e);
            }
        }).orTimeout(30, TimeUnit.SECONDS)
                .whenComplete((categories, throwable) -> {
                    this.getUI().ifPresent(ui -> ui.access(() -> {
                        if (throwable != null) {
                            LOG.error("Error loading categories: {}", throwable.getMessage(), throwable);
                            NotificationUtil.showError("Failed to load categories: " + throwable.getMessage());
                        } else {
                            LOG.info("Successfully loaded {} categories", categories.size());
                            this.allCategories = categories;
                            this.updateTreeGrid();
                        }
                    }));
                });
    }

    private void updateTreeGrid() {
        // Find root categories (categories without parent)
        final var rootCategories = this.allCategories.stream()
                .filter(PostCategoryViewDto::isRootCategory)
                .toList();

        this.treeGrid.setItems(rootCategories, this::getChildrenOfCategory);
        this.treeGrid.expandRecursively(rootCategories, 2); // Expand up to 2 levels
    }

    private void updateSearchTreeGrid(final List<PostCategoryViewDto> searchResults) {
        // For search results, we want to show all matching categories
        // If a matching category has a parent that's not in the search results,
        // we show it as a top-level item for better visibility

        // Get all category IDs that exist in the search results
        final var categoryIdsInResults = searchResults.stream()
                .map(cat -> cat.id)
                .collect(java.util.stream.Collectors.toSet());

        // Find categories to show at the top level:
        // 1. Root categories (no parent)
        // 2. Categories whose parent is not in the search results (orphaned in this
        // context)
        final var topLevelCategories = searchResults.stream()
                .filter(cat -> cat.parentId == null || !categoryIdsInResults.contains(cat.parentId))
                .toList();

        this.treeGrid.setItems(topLevelCategories, category -> searchResults.stream()
                .filter(cat -> cat.parentId != null && cat.parentId.equals(category.id))
                .toList());

        // Expand all search results for better visibility
        this.treeGrid.expandRecursively(topLevelCategories, 10);
    }

    private List<PostCategoryViewDto> getChildrenOfCategory(final PostCategoryViewDto parent) {
        return this.allCategories.stream()
                .filter(category -> category.parentId != null && category.parentId.equals(parent.id))
                .toList();
    }

    private void buildUI() {
        this.removeAll();

        final var header = new H2("Post Categories");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createTreeGrid();
        this.categoryDialog = new FormDialog();

        this.add(header, searchLayout, buttonLayout, this.treeGrid);
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(
                e -> {
                    if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                        this.updateTreeGrid();
                    }
                },
                e -> this.searchCategories(),
                "Search by name...",
                "Search Categories");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        return searchLayout;
    }

    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var createButton = new CreateButton(e -> this.openCategoryDialog(null));
        final var refreshButton = new RefreshButton(e -> this.loadCategoriesAsync());

        layout.add(createButton, refreshButton);
        return layout;
    }

    private void createTreeGrid() {
        this.treeGrid = new TreeGrid<>(PostCategoryViewDto.class);
        this.treeGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.treeGrid.setSizeFull();
        this.treeGrid.removeAllColumns();

        // Configure columns
        this.treeGrid.addHierarchyColumn(category -> category.name)
                .setHeader("Category Name")
                .setFlexGrow(3);

        this.treeGrid.addColumn(category -> category.parentName != null ? category.parentName : "N/A")
                .setHeader("Parent")
                .setFlexGrow(2);

        this.treeGrid.addColumn(category -> category.childrenCount)
                .setHeader("Children")
                .setFlexGrow(1);

        this.treeGrid.addColumn(category -> category.postsCount)
                .setHeader("Posts")
                .setFlexGrow(1);

        // Add action column
        this.treeGrid.addComponentColumn(this::createActionButtons)
                .setHeader("Actions")
                .setWidth("150px")
                .setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final PostCategoryViewDto category) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(e -> this.openCategoryDialog(category.toPostCategoryDto()));
        final var deleteButton = new DeleteButton(e -> this.deleteCategory(category.toPostCategoryDto()));

        layout.add(editButton, deleteButton);
        return layout;
    }

    private void openCategoryDialog(final PostCategoryDto category) {
        this.categoryDialog.removeAll();
        this.currentCategory = category != null ? category : new PostCategoryDto();

        this.binder = new Binder<>(PostCategoryDto.class);

        final var title = new H3(category != null ? "Edit Category" : "Create Category");

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // Form fields
        final var nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setInvalid(false); // Clear any previous validation state

        final var parentField = new ComboBox<PostCategoryDto>("Parent Category");
        parentField.setItemLabelGenerator(cat -> cat != null ? cat.name : "N/A");
        parentField.setPlaceholder("(none)");
        parentField.setInvalid(false); // Clear any previous validation state
        if (this.allCategories != null) {
            // Only show categories that are not descendants of the current category
            final var availableParents = this.allCategories.stream()
                    .map(PostCategoryViewDto::toPostCategoryDto)
                    .filter(cat -> category == null || !this.isDescendantOf(cat, category))
                    .filter(cat -> category == null || !cat.id.equals(category.id))
                    .toList();
            parentField.setItems(availableParents);
        }
        // Allow clearing the selection to make it a root category
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
                                // Find the PostCategoryDto from available parents
                                if (this.allCategories != null) {
                                    return this.allCategories.stream()
                                            .map(PostCategoryViewDto::toPostCategoryDto)
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
                                    cat.parent = new PostCategoryDto.ParentField();
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

        final var saveButton = new Button("Save", e -> this.saveCategory());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", e -> this.categoryDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);
        dialogLayout.setSizeFull();

        this.categoryDialog.add(dialogLayout);

        // Load current category data
        this.binder.readBean(this.currentCategory);

        this.categoryDialog.open();
    }

    private boolean isDescendantOf(final PostCategoryDto potential, final PostCategoryDto ancestor) {
        if (potential.parent == null) {
            return false;
        }
        if (potential.parent.id.equals(ancestor.id)) {
            return true;
        }

        // Find the parent in the list and check recursively
        final var parent = this.allCategories.stream()
                .filter(cat -> cat.id.equals(potential.parent.id))
                .map(PostCategoryViewDto::toPostCategoryDto)
                .findFirst()
                .orElse(null);

        if (parent != null) {
            return this.isDescendantOf(parent, ancestor);
        }

        return false;
    }

    private void saveCategory() {
        try {
            this.binder.writeBean(this.currentCategory);

            // Sync parent field
            this.currentCategory.syncParent();

            // Convert DTO to Entity for service call
            final var categoryEntity = new PostCategoryEntity();
            categoryEntity.id = this.currentCategory.id;
            categoryEntity.name = this.currentCategory.name;

            // Set parent if specified
            if (this.currentCategory.parentId != null) {
                final var parentEntity = new PostCategoryEntity();
                parentEntity.id = this.currentCategory.parentId;
                categoryEntity.parent = parentEntity;
            }

            if (this.currentCategory.id == null) {
                this.categoryService.createCategory(categoryEntity);
                NotificationUtil.showSuccess("Category created successfully");
            } else {
                this.categoryService.updateCategory(categoryEntity);
                NotificationUtil.showSuccess("Category updated successfully");
            }

            this.categoryDialog.close();
            this.loadCategoriesAsync();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final Exception e) {
            LOG.error("Unexpected error saving category", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void deleteCategory(final PostCategoryDto category) {
        // Check if category has children
        final boolean hasChildren = this.allCategories.stream()
                .anyMatch(cat -> cat.parentId != null && cat.parentId.equals(category.id));

        if (hasChildren) {
            NotificationUtil
                    .showError("Cannot delete category with subcategories. Please delete or move subcategories first.");
            return;
        }

        try {
            if (this.categoryService.deleteCategory(category.id)) {
                NotificationUtil.showSuccess("Category deleted successfully");
                this.loadCategoriesAsync();
            } else {
                NotificationUtil.showError("Failed to delete category");
            }
        } catch (final Exception e) {
            LOG.error("Unexpected error deleting category", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void searchCategories() {
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
                return this.categoryService.searchCategories(query.trim());
            } catch (final Exception e) {
                LOG.error("Unexpected error searching categories", e);
                throw new RuntimeException("Unexpected error occurred", e);
            }
        }).orTimeout(30, TimeUnit.SECONDS)
                .whenComplete((categories, throwable) -> {
                    this.getUI().ifPresent(ui -> ui.access(() -> {
                        this.searchButton.setEnabled(true);
                        this.searchButton.setText("Search");
                        if (throwable != null) {
                            LOG.error("Error searching categories: {}", throwable.getMessage(), throwable);
                            NotificationUtil.showError(throwable.getCause() != null ? throwable.getCause().getMessage()
                                    : throwable.getMessage());
                        } else {
                            // Store search results and update the tree grid
                            this.updateSearchTreeGrid(categories);
                        }
                    }));
                });
    }
}
