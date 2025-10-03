package de.vptr.aimathtutor.component;

import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;

import de.vptr.aimathtutor.view.HomeView;

public class NavigationTabs extends Tabs {

    public NavigationTabs() {
        this.add(new Tab(new RouterLink("Home", HomeView.class)));
        // this.add(new Tab(new RouterLink("Exercises", Exercises.class)));
        // this.add(new Tab(new RouterLink("Lessons", Lessons.class)));
        // this.add(new Tab(new RouterLink("Users", UserView.class)));
        // this.add(new Tab(new RouterLink("Groups", UserGroupView.class)));
    }
}
