package de.vptr.aimathtutor.view.admin;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

import de.vptr.aimathtutor.service.AuthService;
import de.vptr.aimathtutor.service.UserRankService;
import de.vptr.aimathtutor.view.LoginView;
import jakarta.inject.Inject;

/**
 * Abstract base class for admin views providing standardized authentication
 * and authorization checks.
 *
 * <p>Subclasses should call {@code super.beforeEnter(event)} at the start of
 * their own {@code beforeEnter} override, or rely on the default behaviour
 * which verifies the user is authenticated and authorised before initialising
 * the view.</p>
 */
public abstract class AbstractAdminView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    protected transient AuthService authService;

    @Inject
    protected transient UserRankService userRankService;

    /**
     * Performs authentication and authorization checks before the view is shown.
     * Redirects unauthenticated users to the login page and unauthorised users
     * to the home page.
     *
     * @param event the before-enter navigation event
     */
    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        this.isAuthOk(event);
    }

    /**
     * Checks authentication and authorization. Returns {@code true} if the user
     * may proceed, otherwise forwards the navigation and returns {@code false}.
     *
     * <p>Subclasses overriding {@code beforeEnter} should call this method and
     * return early when it yields {@code false}:</p>
     *
     * <pre>
     * if (!isAuthOk(event)) {
     *     return;
     * }
     * </pre>
     *
     * @param event the before-enter navigation event
     * @return true if the user is authenticated and authorised
     */
    protected boolean isAuthOk(final BeforeEnterEvent event) {
        if (!this.authService.isAuthenticated()) {
            event.forwardTo(LoginView.class);
            return false;
        }
        if (!this.isAuthorized()) {
            event.forwardTo("");
            return false;
        }
        return true;
    }

    /**
     * Determines whether the current user is authorised to access this admin view.
     * The default implementation requires {@code canAdminView()} on the user's rank.
     *
     * <p>Subclasses with different permission requirements (e.g.
     * {@link AdminConfigView}) can override this method.</p>
     *
     * @return true if the user is authorised, false otherwise
     */
    protected boolean isAuthorized() {
        final var userRank = this.userRankService.getCurrentUserRank();
        return userRank != null && userRank.canAdminView();
    }
}
