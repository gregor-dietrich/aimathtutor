package de.vptr.aimathtutor.component;

import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;

import de.vptr.aimathtutor.dto.Permission;
import de.vptr.aimathtutor.dto.UserRankViewDto;
import de.vptr.aimathtutor.view.admin.AdminCommentsView;
import de.vptr.aimathtutor.view.admin.AdminConfigView;
import de.vptr.aimathtutor.view.admin.AdminDashboardView;
import de.vptr.aimathtutor.view.admin.AdminExercisesView;
import de.vptr.aimathtutor.view.admin.AdminLessonsView;
import de.vptr.aimathtutor.view.admin.AdminProgressView;
import de.vptr.aimathtutor.view.admin.AdminSessionsView;
import de.vptr.aimathtutor.view.admin.AdminUserGroupsView;
import de.vptr.aimathtutor.view.admin.AdminUserRanksView;
import de.vptr.aimathtutor.view.admin.AdminUsersView;

/**
 * Vertical navigation tabs used in the admin area. Tabs are created based on the provided user's rank/permissions.
 */
public class AdminNavigationTabs extends Tabs {

    /**
     * Constructs AdminNavigationTabs with tabs based on user rank permissions.
     *
     * @param userRank
     *            the user's rank and permissions
     */
    public AdminNavigationTabs(final UserRankViewDto userRank) {
        this.setOrientation(Orientation.VERTICAL);
        this.setWidthFull();

        if (userRank.hasAny(Permission.Group.EXERCISE) || userRank.hasAny(Permission.Group.LESSON)) {
            this.add(new Tab(new RouterLink("Dashboard", AdminDashboardView.class)));
            this.add(new Tab(new RouterLink("Configuration", AdminConfigView.class)));
            this.add(new Tab(new RouterLink("Sessions", AdminSessionsView.class)));
            this.add(new Tab(new RouterLink("Progress", AdminProgressView.class)));
        }

        if (userRank.hasAny(Permission.Group.EXERCISE)) {
            this.add(new Tab(new RouterLink("Exercises", AdminExercisesView.class)));
        }

        if (userRank.hasAny(Permission.Group.LESSON)) {
            this.add(new Tab(new RouterLink("Lessons", AdminLessonsView.class)));
        }

        if (userRank.hasAny(Permission.Group.COMMENT)) {
            this.add(new Tab(new RouterLink("Comments", AdminCommentsView.class)));
        }

        if (userRank.hasAny(Permission.Group.USER)) {
            this.add(new Tab(new RouterLink("Users", AdminUsersView.class)));
        }

        if (userRank.hasAny(Permission.Group.USER_GROUP)) {
            this.add(new Tab(new RouterLink("User Groups", AdminUserGroupsView.class)));
        }

        if (userRank.hasAny(Permission.Group.USER_RANK)) {
            this.add(new Tab(new RouterLink("User Ranks", AdminUserRanksView.class)));
        }
    }
}
