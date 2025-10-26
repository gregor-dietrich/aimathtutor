package de.vptr.aimathtutor.component;

import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;

import de.vptr.aimathtutor.dto.UserRankViewDto;
import de.vptr.aimathtutor.view.admin.*;

public class AdminNavigationTabs extends Tabs {

    public AdminNavigationTabs(final UserRankViewDto userRank) {
        this.setOrientation(Orientation.VERTICAL);
        this.setWidthFull();

        if (userRank.hasAnyExercisePermission() || userRank.hasAnyLessonPermission()) {
            this.add(new Tab(new RouterLink("Dashboard", AdminDashboardView.class)));
            this.add(new Tab(new RouterLink("Sessions", AdminSessionsView.class)));
            this.add(new Tab(new RouterLink("Progress", AdminProgressView.class)));
        }

        if (userRank.hasAnyExercisePermission()) {
            this.add(new Tab(new RouterLink("Exercises", AdminExercisesView.class)));
        }

        if (userRank.hasAnyLessonPermission()) {
            this.add(new Tab(new RouterLink("Lessons", AdminLessonsView.class)));
        }

        if (userRank.hasAnyCommentPermission()) {
            this.add(new Tab(new RouterLink("Comments", AdminCommentsView.class)));
        }

        if (userRank.hasAnyUserPermission()) {
            this.add(new Tab(new RouterLink("Users", AdminUsersView.class)));
        }

        if (userRank.hasAnyUserGroupPermission()) {
            this.add(new Tab(new RouterLink("User Groups", AdminUserGroupsView.class)));
        }

        if (userRank.hasAnyUserRankPermission()) {
            this.add(new Tab(new RouterLink("User Ranks", AdminUserRanksView.class)));
        }
    }
}
