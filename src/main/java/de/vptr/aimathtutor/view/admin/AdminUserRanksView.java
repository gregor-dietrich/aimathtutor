package de.vptr.aimathtutor.view.admin;

import java.util.EnumMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.component.button.CreateButton;
import de.vptr.aimathtutor.component.button.DeleteButton;
import de.vptr.aimathtutor.component.button.EditButton;
import de.vptr.aimathtutor.component.button.RefreshButton;
import de.vptr.aimathtutor.component.dialog.FormDialog;
import de.vptr.aimathtutor.component.layout.SearchLayout;
import de.vptr.aimathtutor.dto.Permission;
import de.vptr.aimathtutor.dto.UserRankDto;
import de.vptr.aimathtutor.dto.UserRankViewDto;
import de.vptr.aimathtutor.exception.PermissionDeniedException;
import de.vptr.aimathtutor.util.AppConstants;
import de.vptr.aimathtutor.util.AppStyles;
import de.vptr.aimathtutor.util.AsyncDataLoader;
import de.vptr.aimathtutor.util.NotificationUtil;
import de.vptr.aimathtutor.util.ServiceRejectionUtil;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Admin view for creating, editing and deleting user ranks (permission sets).
 */
@Route(value = "admin/user-ranks", layout = AdminMainLayout.class)
@SuppressWarnings("NullAway")
public class AdminUserRanksView extends AbstractAdminView {

    private static final Logger LOG = Logger.getLogger(AdminUserRanksView.class);

    private Grid<UserRankViewDto> grid;
    private TextField searchField;
    private Button searchButton;

    private Dialog rankDialog;
    private Binder<UserRankDto> binder;
    private transient UserRankDto currentRank;
    private final transient Map<Permission, Checkbox> permissionFields = new EnumMap<>(Permission.class);

    /**
     * Constructs the AdminUserRanksView with full size and padding.
     */
    public AdminUserRanksView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    @Override
    protected void onAttach(final AttachEvent event) {
        super.onAttach(event);
        this.loadRanksAsync();
    }

    private void loadRanksAsync() {
        LOG.info("Starting async rank loading");
        AsyncDataLoader.load(() -> this.userRankService.getAllRanks(), this, ranks -> this.grid.setItems(ranks),
                "Failed to load ranks. Please try again.");
    }

    /**
     * Construct UI elements for the user ranks admin view.
     */
    @Override
    protected void buildUi() {
        this.removeAll();

        final var header = new H2("User Ranks");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createGrid();
        this.rankDialog = new FormDialog();

        this.add(header, searchLayout, buttonLayout, this.grid);
    }

    /**
     * Create the search layout for searching ranks.
     *
     * @return the created search layout
     */
    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(e -> {
            if (e.getValue() == null || e.getValue().isBlank()) {
                this.loadRanksAsync();
            }
        }, e -> this.searchRanks(), "Search by name...", "Search Ranks");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        return searchLayout;
    }

    /**
     * Create the button layout containing create/refresh actions.
     *
     * @return the horizontal layout with action buttons
     */
    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var createButton = new CreateButton(e -> this.openRankDialog(null));
        final var refreshButton = new RefreshButton(e -> this.loadRanksAsync());

        layout.add(createButton, refreshButton);
        return layout;
    }

    private void createGrid() {
        this.grid = new Grid<>(UserRankViewDto.class, false);
        this.grid.addThemeVariants(GridVariant.ROW_STRIPES);
        this.grid.setSizeFull();

        // Make the name column clickable, except on ranks beyond the caller's
        this.grid.addComponentColumn(rank -> {
            final var nameSpan = new Span(rank.name);
            if (this.beyondCaller(rank)) {
                return nameSpan;
            }
            nameSpan.getStyle().set("color", AppStyles.ACCENT_TEXT);
            nameSpan.getStyle().set("cursor", "pointer");
            nameSpan.getStyle().set("width", "100%");
            nameSpan.getStyle().set("display", "block");
            nameSpan.addClickListener(e -> this.openRankDialog(rank));
            return nameSpan;
        }).setHeader("Name").setFlexGrow(2);

        // One column per permission group, showing an icon for each granted permission
        for (final var group : Permission.Group.values()) {
            this.addPermissionColumn(group);
        }

        this.grid.addComponentColumn(this::createActionButtons).setHeader("Actions")
                .setWidth(AppConstants.GRID_ACTION_WIDTH).setFlexGrow(0);
    }

    /**
     * Append a column showing one icon per permission of the group that the rank holds.
     *
     * @param group
     *            the permission group the column shows
     */
    private void addPermissionColumn(final Permission.Group group) {
        final var permissions = group.permissions();
        this.grid.addComponentColumn(rank -> {
            final var layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setPadding(false);
            permissions.stream().filter(rank::has).forEach(permission -> layout.add(this.permissionIcon(permission)));
            return layout;
        }).setHeader(group.columnHeader()).setWidth(permissions.size() == 1 ? "110px" : AppConstants.GRID_ACTION_WIDTH)
                .setFlexGrow(0);
    }

    private SvgIcon permissionIcon(final Permission permission) {
        final var icon = switch (permission.operation()) {
            case VIEW -> LineAwesomeIcon.CHECK_SOLID;
            case ADD -> LineAwesomeIcon.PLUS_SOLID;
            case EDIT -> LineAwesomeIcon.EDIT_SOLID;
            case DELETE -> LineAwesomeIcon.TRASH_ALT_SOLID;
        };
        final var svg = icon.create();
        svg.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
        svg.setTooltipText(permission.label());
        svg.getElement().setAttribute("aria-label", permission.label());
        return svg;
    }

    /**
     * Create action buttons for a rank row (edit/delete).
     *
     * @param rank
     *            the rank dto
     * @return a horizontal layout with action buttons
     */
    private HorizontalLayout createActionButtons(final UserRankViewDto rank) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(e -> this.openRankDialog(rank));
        final var deleteButton = new DeleteButton(e -> this.deleteRank(rank));

        // A rank granting beyond the caller's own permissions is read-only here; the service refuses it anyway
        final var editable = !this.beyondCaller(rank);
        editButton.setEnabled(editable);
        deleteButton.setEnabled(editable);

        layout.add(editButton, deleteButton);
        return layout;
    }

    private boolean beyondCaller(final UserRankViewDto rank) {
        return rank.grantsBeyond(this.callerPermissions());
    }

    private TextField createBoundNameField() {
        final var nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setInvalid(false); // Clear any previous validation state
        this.binder.forField(nameField).withValidator(value -> value != null && !value.isBlank(), "Name is required")
                .bind(rank1 -> rank1.name, (rank1, value) -> rank1.name = value);
        return nameField;
    }

    /**
     * Open a dialog to edit or create a user rank.
     *
     * @param rank
     *            the rank to edit or null to create a new one
     */
    private void openRankDialog(@Nullable final UserRankViewDto rank) {
        this.rankDialog.removeAll();
        this.currentRank = rank != null ? rank.toUserRankDto() : new UserRankDto();

        this.binder = new Binder<>(UserRankDto.class);

        final var title = new H3(rank != null ? "Edit Rank" : "Create Rank");

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        form.add(this.createBoundNameField());

        // One section per permission group; the checkboxes stay in a map for callers that enable or disable them
        this.permissionFields.clear();
        for (final var group : Permission.Group.values()) {
            form.add(new H3(group.sectionTitle()));
            for (final var permission : group.permissions()) {
                final var checkbox = new Checkbox(permission.label());
                this.binder.bind(checkbox, rank1 -> rank1.get(permission),
                        (rank1, value) -> rank1.set(permission, value));
                checkbox.setEnabled(this.callerPermissions().contains(permission));
                this.permissionFields.put(permission, checkbox);
                form.add(checkbox);
            }
        }

        // Button layout
        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Save", e -> this.saveRank());
        saveButton.addThemeVariants(ButtonVariant.PRIMARY);

        final var cancelButton = new Button("Cancel", e -> this.rankDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);
        dialogLayout.setSizeFull();

        // Make the form scrollable
        form.getStyle().set("overflow-y", "auto");
        form.setMaxHeight("400px");

        this.rankDialog.add(dialogLayout);

        // Load current rank data
        this.binder.readBean(this.currentRank);

        this.rankDialog.open();
    }

    private void saveRank() {
        try {
            // Validate the form before attempting to save
            if (!this.binder.validate().isOk()) {
                NotificationUtil.showError("Please check the form for errors");
                return;
            }

            this.binder.writeBean(this.currentRank);

            final var saved = ServiceRejectionUtil.runOrShowRejection(() -> {
                if (this.currentRank.publicId == null) {
                    this.userRankService.createRank(this.currentRank);
                    NotificationUtil.showSuccess("Rank created successfully");
                } else {
                    this.userRankService.updateRank(this.currentRank.publicId, this.currentRank);
                    NotificationUtil.showSuccess("Rank updated successfully");
                }
            });
            if (saved) {
                this.rankDialog.close();
                this.loadRanksAsync();
            }

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final PermissionDeniedException e) {
            NotificationUtil.showError(e.getMessage() != null ? e.getMessage() : "Permission denied");
        } catch (final Exception e) {
            LOG.error("Unexpected error saving rank", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void deleteRank(final UserRankViewDto rank) {
        try {
            ServiceRejectionUtil.runOrShowRejection(() -> {
                if (rank.publicId != null && this.userRankService.deleteRank(rank.publicId)) {
                    NotificationUtil.showSuccess("Rank deleted successfully");
                    this.loadRanksAsync();
                } else {
                    NotificationUtil.showError("Failed to delete rank");
                }
            });
        } catch (final PermissionDeniedException e) {
            NotificationUtil.showError(e.getMessage() != null ? e.getMessage() : "Permission denied");
        } catch (final WebApplicationException e) {
            // CONFLICT carries a user-facing reason (users still assigned); other statuses are unexpected
            final String reason = e.getMessage();
            if (e.getResponse().getStatus() == Response.Status.CONFLICT.getStatusCode() && reason != null) {
                NotificationUtil.showError(reason);
            } else {
                LOG.error("Error deleting rank", e);
                NotificationUtil.showError("Failed to delete rank. Please try again.");
            }
        } catch (final Exception e) {
            LOG.error("Unexpected error deleting rank", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void searchRanks() {
        final String query = this.searchField.getValue();
        if (query == null || query.isBlank()) {
            this.loadRanksAsync();
            return;
        }

        LOG.debugf("Starting async rank search with query length: %s", query.length());
        this.loadSearch(this.searchButton, () -> this.userRankService.searchRanks(query),
                ranks -> this.grid.setItems(ranks), "Failed to search ranks. Please try again.");
    }
}
