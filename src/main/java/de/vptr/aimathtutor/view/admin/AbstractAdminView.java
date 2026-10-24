package de.vptr.aimathtutor.view.admin;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

import de.vptr.aimathtutor.dto.Permission;
import de.vptr.aimathtutor.dto.UserRankViewDto;
import de.vptr.aimathtutor.service.UserRankService;
import de.vptr.aimathtutor.service.security.AuthService;
import de.vptr.aimathtutor.util.AsyncDataLoader;
import de.vptr.aimathtutor.view.LoginView;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/**
 * Abstract base class for admin views providing standardized authentication and authorization checks.
 *
 * <p>
 * Subclasses should call {@code super.beforeEnter(event)} at the start of their own {@code beforeEnter} override, or
 * rely on the default behaviour which verifies the user is authenticated and authorised before initialising the view.
 * </p>
 */
public abstract class AbstractAdminView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(AbstractAdminView.class);

    @Inject
    protected transient AuthService authService;

    @Inject
    protected transient UserRankService userRankService;

    /**
     * The signed-in user's rank, as {@link #isAuthOk} loaded it for this navigation; null before that, after
     * deserialization, or if they have none. A null value fails closed: {@link #isAuthorized()} refuses it and views
     * offer no permission-gated control. It decides which controls a view offers; the services still check every
     * action.
     */
    @Nullable
    protected transient UserRankViewDto callerRank;

    /**
     * The permissions the signed-in user's rank holds, which cap what a view offers; empty before {@link #callerRank}
     * is loaded or if they have no rank, so a view offers nothing it cannot confirm.
     *
     * @return the caller's permissions, never null
     */
    protected Set<Permission> callerPermissions() {
        return this.callerRank == null ? Set.of() : this.callerRank.granted();
    }

    /**
     * Performs authentication and authorization checks before the view is shown. Redirects unauthenticated users to the
     * login page and unauthorised users to the home page. If authorized, triggers the {@link #onBeforeEnter} hook and
     * {@link #buildUi()}.
     *
     * @param event
     *            the before-enter navigation event
     */
    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (this.isAuthOk(event)) {
            this.onBeforeEnter(event);
            this.buildUi();
        }
    }

    /**
     * Hook for subclasses to perform logic before UI construction (e.g. extracting route parameters).
     *
     * @param event
     *            the before-enter navigation event
     */
    protected void onBeforeEnter(final BeforeEnterEvent event) {
        LOG.tracef("Entering admin view: %s", this.getClass().getSimpleName());
    }

    /**
     * Constructs the UI for this admin view. Called automatically by {@link #beforeEnter(BeforeEnterEvent)} after
     * security checks pass.
     */
    protected abstract void buildUi();

    /**
     * Checks authentication and authorization. Returns {@code true} if the user may proceed, otherwise forwards the
     * navigation and returns {@code false}.
     *
     * <p>
     * Subclasses overriding {@code beforeEnter} should call this method and return early when it yields {@code false}:
     * </p>
     *
     * <pre>
     * if (!isAuthOk(event)) {
     *     return;
     * }
     * </pre>
     *
     * @param event
     *            the before-enter navigation event
     * @return true if the user is authenticated and authorised
     */
    protected boolean isAuthOk(final BeforeEnterEvent event) {
        if (!this.authService.isAuthenticated()) {
            event.forwardTo(LoginView.class);
            return false;
        }
        this.refreshCallerRank();
        if (!this.isAuthorized()) {
            event.forwardTo("");
            return false;
        }
        return true;
    }

    /**
     * Determines whether the current user is authorised to access this admin view. The default implementation requires
     * {@code has(Permission.ADMIN_VIEW)} on the user's rank, which {@link #isAuthOk} has loaded into {@link #callerRank}
     * (once per navigation).
     *
     * <p>
     * Subclasses with different permission requirements (e.g. {@link AdminConfigView}) can override this method.
     * </p>
     *
     * @return true if the user is authorised, false otherwise
     */
    protected boolean isAuthorized() {
        return this.callerRank != null && this.callerRank.has(Permission.ADMIN_VIEW);
    }

    /**
     * Re-reads the signed-in user's rank into {@link #callerRank}. Call it on the UI thread only: the rank comes from
     * the {@code VaadinSession}, which a worker thread cannot see (it would read null and disable every action).
     *
     * @return the caller's rank, or null if there is none
     */
    @Nullable
    protected UserRankViewDto refreshCallerRank() {
        this.callerRank = this.userRankService.getCurrentUserRank();
        return this.callerRank;
    }

    /**
     * Creates a required "Name" field bound to the given binder, refusing a blank name.
     *
     * @param <T>
     *            the bound bean type
     * @param binder
     *            the dialog's binder
     * @param getter
     *            reads the name from the bean
     * @param setter
     *            writes the name to the bean
     * @return the bound name field
     */
    protected static <T> TextField createNameField(final Binder<T> binder, final ValueProvider<T, String> getter,
            final Setter<T, String> setter) {
        final var nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setInvalid(false); // Clear any previous validation state
        binder.forField(nameField).withValidator(value -> value != null && !value.isBlank(), "Name is required")
                .bind(getter, setter);
        return nameField;
    }

    /**
     * Runs a search through {@link AsyncDataLoader}, with the search button disabled and labelled "Searching..." until
     * the search succeeds or fails, then restores the button's own label.
     *
     * @param <T>
     *            the search result type
     * @param searchButton
     *            the button that started the search
     * @param search
     *            the blocking search call
     * @param onSuccess
     *            applies the result on the UI thread
     * @param errorMessage
     *            the message shown when the search fails
     */
    protected <T> void loadSearch(final Button searchButton, final Supplier<T> search, final Consumer<T> onSuccess,
            final String errorMessage) {
        final var label = searchButton.getText();
        searchButton.setEnabled(false);
        searchButton.setText("Searching...");
        final Runnable reset = () -> {
            searchButton.setEnabled(true);
            searchButton.setText(label);
        };
        AsyncDataLoader.load(search, this, result -> {
            reset.run();
            onSuccess.accept(result);
        }, reset, errorMessage);
    }
}
