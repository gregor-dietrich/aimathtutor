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
        this.add(new Tab(new RouterLink("Home", AdminHomeView.class)));

        if (userRank.hasAnyPostPermission()) {
            this.add(new Tab(new RouterLink("Exercises", AdminExerciseView.class)));
        }

        if (userRank.hasAnyPostCategoryPermission()) {
            this.add(new Tab(new RouterLink("Lessons", AdminLessonView.class)));
        }

        if (userRank.hasAnyPostCommentPermission()) {
            this.add(new Tab(new RouterLink("Comments", AdminCommentView.class)));
        }

        if (userRank.hasAnyUserPermission()) {
            this.add(new Tab(new RouterLink("Users", AdminUserView.class)));
        }

        if (userRank.hasAnyUserGroupPermission()) {
            this.add(new Tab(new RouterLink("User Groups", AdminUserGroupView.class)));
        }

        if (userRank.hasAnyUserRankPermission()) {
            this.add(new Tab(new RouterLink("User Ranks", AdminUserRankView.class)));
        }
    }
}
