package de.vptr.aimathtutor.view.admin;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.component.button.*;
import de.vptr.aimathtutor.component.dialog.FormDialog;
import de.vptr.aimathtutor.component.layout.DateFilterLayout;
import de.vptr.aimathtutor.component.layout.IntegerFilterLayout;
import de.vptr.aimathtutor.component.layout.SearchLayout;
import de.vptr.aimathtutor.rest.dto.PostCategoryViewDto;
import de.vptr.aimathtutor.rest.dto.PostCommentDto;
import de.vptr.aimathtutor.rest.dto.PostDto;
import de.vptr.aimathtutor.rest.dto.PostViewDto;
import de.vptr.aimathtutor.rest.entity.PostCommentEntity;
import de.vptr.aimathtutor.rest.entity.PostEntity;
import de.vptr.aimathtutor.rest.service.*;
import de.vptr.aimathtutor.util.NotificationUtil;
import de.vptr.aimathtutor.view.LoginView;
import jakarta.inject.Inject;

@Route(value = "admin/posts", layout = AdminMainLayout.class)
public class AdminPostView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger LOG = LoggerFactory.getLogger(AdminPostView.class);

    @Inject
    PostService postService;

    @Inject
    PostCategoryService postCategoryService;

    @Inject
    AuthService authService;

    @Inject
    UserService userService;

    @Inject
    PostCommentService postCommentService;

    private Grid<PostViewDto> grid;
    private TextField searchField;
    private Button searchButton;
    private Button showPublishedButton;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private IntegerField userIdField;

    private Dialog postDialog;
    private Binder<PostDto> binder;
    private PostDto currentPost;
    private List<PostCategoryViewDto> availableCategories;

    // Comment management components
    private Dialog commentDialog;
    private Binder<PostCommentDto> commentBinder;
    private PostCommentDto currentComment;
    private PostViewDto selectedPost;

    public AdminPostView() {
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
        this.loadPostsAsync();
    }

    private void loadPostsAsync() {
        LOG.info("Loading posts");
        try {
            final var posts = this.postService.getAllPosts();
            LOG.info("Successfully loaded {} posts", posts.size());
            this.grid.setItems(posts);
        } catch (final Exception e) {
            LOG.error("Error loading posts", e);
            NotificationUtil.showError("Failed to load posts: " + e.getMessage());
        }
    }

    private void loadPublishedPostsAsync() {
        LOG.info("Loading published posts");
        try {
            final var posts = this.postService.getPublishedPosts();
            LOG.info("Successfully loaded {} published posts", posts.size());
            this.grid.setItems(posts);
        } catch (final Exception e) {
            LOG.error("Error loading published posts", e);
            NotificationUtil.showError("Failed to load published posts: " + e.getMessage());
        }
    }

    private void loadCategoriesAsync() {
        LOG.info("Loading categories");
        try {
            this.availableCategories = this.postCategoryService.getAllCategories();
            LOG.info("Successfully loaded {} categories", this.availableCategories.size());
        } catch (final Exception e) {
            LOG.error("Error loading categories", e);
            this.availableCategories = List.of(); // Empty list as fallback
        }
    }

    private void buildUI() {
        this.removeAll();

        final var header = new H2("Posts");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createGrid();
        this.postDialog = new FormDialog();
        this.commentDialog = new FormDialog();

        this.add(header, searchLayout, buttonLayout, this.grid);
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(
                e -> {
                    if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                        this.loadPostsAsync();
                    }
                },
                e -> this.searchPosts(),
                "Search by title or content...",
                "Search Posts");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        this.showPublishedButton = new Button("Show Published Only", e -> this.loadPublishedPostsAsync());
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

        final var createButton = new CreateButton(e -> this.openPostDialog(null));
        final var refreshButton = new RefreshButton(e -> this.loadPostsAsync());

        layout.add(createButton, refreshButton);
        return layout;
    }

    private void createGrid() {
        this.grid = new Grid<>(PostViewDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        // Configure columns
        this.grid.addColumn(post -> post.id).setHeader("ID").setWidth("80px").setFlexGrow(0);

        // Make the title column clickable
        this.grid.addComponentColumn(post -> {
            final var titleSpan = new Span(post.title);
            titleSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            titleSpan.getStyle().set("cursor", "pointer");
            titleSpan.getStyle().set("width", "100%");
            titleSpan.getStyle().set("display", "block");
            titleSpan.addClickListener(e -> this.openPostDialog(post));
            return titleSpan;
        }).setHeader("Title").setFlexGrow(2);

        this.grid.addColumn(post -> post.username != null ? post.username : "").setHeader("Author").setWidth("120px")
                .setFlexGrow(0);
        this.grid.addColumn(post -> post.categoryName != null ? post.categoryName : "").setHeader("Category")
                .setWidth("120px").setFlexGrow(0);

        this.grid.addComponentColumn(post -> {
            final var checkbox = new Checkbox();
            checkbox.setValue(post.published != null ? post.published : false);
            checkbox.setReadOnly(true);
            return checkbox;
        }).setHeader("Published").setWidth("100px").setFlexGrow(0);

        this.grid.addComponentColumn(post -> {
            final var checkbox = new Checkbox();
            checkbox.setValue(post.commentable != null ? post.commentable : false);
            checkbox.setReadOnly(true);
            return checkbox;
        }).setHeader("Commentable").setWidth("100px").setFlexGrow(0);

        this.grid.addColumn(post -> post.created).setHeader("Created").setWidth("150px").setFlexGrow(0);
        this.grid.addColumn(post -> post.lastEdit).setHeader("Last Edit").setWidth("150px").setFlexGrow(0);

        // Add action column
        this.grid.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("200px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final PostViewDto post) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(e -> this.openPostDialog(post));
        final var deleteButton = new DeleteButton(e -> this.deletePost(post));
        final var commentButton = new AddCommentButton(e -> this.openCommentDialog(post));

        layout.add(editButton, deleteButton, commentButton);
        return layout;
    }

    private void openPostDialog(final PostViewDto post) {
        this.postDialog.removeAll();
        this.currentPost = post != null ? post.toPostDto() : new PostDto();

        this.binder = new Binder<>(PostDto.class);

        // For new posts, automatically set the current user as the author
        if (post == null) {
            try {
                final var currentUser = this.userService.getCurrentUser();
                this.currentPost.userId = currentUser.id;
                this.currentPost.user = new PostDto.UserField();
                this.currentPost.user.setId(currentUser.id);
                this.currentPost.user.setUsername(currentUser.username);
            } catch (final Exception e) {
                LOG.error("Error retrieving current user for new post", e);
                NotificationUtil.showError("Error retrieving user information. Please try again.");
                return;
            }
        }

        final var title = new H3(post != null ? "Edit Post" : "Create Post");

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

        // Category dropdown
        final var categoryField = new ComboBox<PostCategoryViewDto>("Category");
        categoryField.setItems(this.availableCategories != null ? this.availableCategories : List.of());
        categoryField.setItemLabelGenerator(PostCategoryViewDto::getName);
        categoryField.setPlaceholder("(none)");
        categoryField.setClearButtonVisible(true);
        categoryField.setInvalid(false); // Clear any previous validation state

        // Bind fields
        this.binder.forField(titleField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Title is required")
                .bind(post1 -> post1.title, (post1, value) -> post1.title = value);
        this.binder.forField(contentField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Content is required")
                .bind(post1 -> post1.content, (post1, value) -> post1.content = value);
        this.binder.bind(publishedField, post1 -> post1.published != null ? post1.published : false,
                (post1, value) -> post1.published = value);
        this.binder.bind(commentableField, post1 -> post1.commentable != null ? post1.commentable : false,
                (post1, value) -> post1.commentable = value);

        // Category binding - convert between PostCategoryViewDto and categoryId
        this.binder.bind(categoryField,
                post1 -> {
                    if (post1.categoryId != null && this.availableCategories != null) {
                        return this.availableCategories.stream()
                                .filter(cat -> cat.getId().equals(post1.categoryId))
                                .findFirst()
                                .orElse(null);
                    }
                    return null;
                },
                (post1, value) -> {
                    if (value != null) {
                        post1.categoryId = value.getId();
                        // Also update the category object for consistency
                        if (post1.category == null) {
                            post1.category = new PostDto.CategoryField();
                        }
                        post1.category.id = value.getId();
                        post1.category.name = value.getName();
                    } else {
                        post1.categoryId = null;
                        post1.category = null;
                    }
                });

        form.add(titleField, contentField, categoryField, publishedField, commentableField);

        // Button layout
        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Save", e -> this.savePost());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", e -> this.postDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);
        dialogLayout.setSizeFull();

        this.postDialog.add(dialogLayout);

        // Load current post data
        this.binder.readBean(this.currentPost);

        this.postDialog.open();
    }

    private void savePost() {
        try {
            // Validate the form before attempting to save
            if (!this.binder.validate().isOk()) {
                NotificationUtil.showError("Please check the form for errors");
                return;
            }

            this.binder.writeBean(this.currentPost);

            // Clear timestamp fields to let the backend handle them
            // This prevents issues with timestamp format mismatches
            this.currentPost.created = null;
            this.currentPost.lastEdit = null;

            if (this.currentPost.id == null) {
                this.postService.createPost(this.currentPost);
                NotificationUtil.showSuccess("Post created successfully");
            } else {
                this.postService.updatePost(this.currentPost.id, this.currentPost);
                NotificationUtil.showSuccess("Post updated successfully");
            }

            this.postDialog.close();
            this.loadPostsAsync();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final Exception e) {
            LOG.error("Unexpected error saving post", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void deletePost(final PostViewDto post) {
        try {
            if (this.postService.deletePost(post.id)) {
                NotificationUtil.showSuccess("Post deleted successfully");
                this.loadPostsAsync();
            } else {
                NotificationUtil.showError("Failed to delete post");
            }
        } catch (final Exception e) {
            LOG.error("Unexpected error deleting post", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void searchPosts() {
        final String query = this.searchField.getValue();
        if (query == null || query.trim().isEmpty()) {
            NotificationUtil.showWarning("Please enter a search query");
            return;
        }
        this.searchButton.setEnabled(false);
        this.searchButton.setText("Searching...");
        try {
            final var posts = this.postService.searchPosts(query.trim());
            this.grid.setItems(posts);
        } catch (final Exception e) {
            LOG.error("Error searching posts", e);
            NotificationUtil.showError("Error searching posts: " + e.getMessage());
        } finally {
            this.searchButton.setEnabled(true);
            this.searchButton.setText("Search");
        }
    }

    private void openCommentDialog(final PostViewDto post) {
        this.selectedPost = post;
        this.commentDialog.removeAll();
        this.currentComment = new PostCommentDto();

        this.commentBinder = new Binder<>(PostCommentDto.class);

        final var title = new H3("Add Comment to: " + post.title);

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // Form fields
        final var contentField = new TextArea("Comment");
        contentField.setRequired(true);
        contentField.setWidthFull();
        contentField.setHeight("200px");
        contentField.setPlaceholder("Write your comment here...");
        contentField.setInvalid(false); // Clear any previous validation state

        // Bind fields
        this.commentBinder.forField(contentField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Comment is required")
                .bind(comment -> comment.content, (comment, value) -> comment.content = value);

        form.add(contentField);

        // Button layout
        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Post Comment", e -> this.saveComment());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", e -> this.commentDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);
        dialogLayout.setSizeFull();

        this.commentDialog.add(dialogLayout);

        // Load current comment data
        this.commentBinder.readBean(this.currentComment);

        this.commentDialog.open();
    }

    private void saveComment() {
        try {
            // Validate the form before attempting to save
            if (!this.commentBinder.validate().isOk()) {
                NotificationUtil.showError("Please check the form for errors");
                return;
            }

            this.commentBinder.writeBean(this.currentComment);

            // Convert DTO to Entity for service call
            final var commentEntity = new PostCommentEntity();
            commentEntity.content = this.currentComment.content;

            // Set the post reference
            final var postEntity = new PostEntity();
            postEntity.id = this.selectedPost.id;
            commentEntity.post = postEntity;

            // Get current username from session
            final var session = com.vaadin.flow.server.VaadinSession.getCurrent();
            final var currentUsername = (String) session.getAttribute("authenticated.username");

            this.postCommentService.createComment(commentEntity, currentUsername);
            NotificationUtil.showSuccess("Comment added successfully");

            this.commentDialog.close();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final Exception e) {
            LOG.error("Error creating comment", e);
            NotificationUtil.showError("Error creating comment: " + e.getMessage());
        }
    }

    private void filterByDateRange() {
        final LocalDate startDate = this.startDatePicker.getValue();
        final LocalDate endDate = this.endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            NotificationUtil.showWarning("Please select both start and end dates");
            return;
        }

        if (startDate.isAfter(endDate)) {
            NotificationUtil.showWarning("Start date must be before end date");
            return;
        }

        try {
            final var posts = this.postService.getPostsByDateRange(startDate.toString(), endDate.toString());
            this.grid.setItems(posts);
        } catch (final Exception e) {
            LOG.error("Error filtering posts by date range", e);
            NotificationUtil.showError("Error filtering posts: " + e.getMessage());
        }
    }

    private void filterByUser() {
        final Integer userId = this.userIdField.getValue();
        if (userId == null) {
            NotificationUtil.showWarning("Please enter a user ID");
            return;
        }

        try {
            final var posts = this.postService.getPostsByUser(userId.longValue());
            this.grid.setItems(posts);
        } catch (final Exception e) {
            LOG.error("Error filtering posts by user", e);
            NotificationUtil.showError("Error filtering posts: " + e.getMessage());
        }
    }
}
