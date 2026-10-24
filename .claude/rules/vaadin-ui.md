---
description: "Use when writing, modifying, or reviewing Vaadin views, layouts or components. Covers routing, the admin view base, async loading and the shared components. The Vaadin rules that hold everywhere stay in AGENTS.md."
paths:
  - "src/main/java/de/vptr/aimathtutor/view/**"
  - "src/main/java/de/vptr/aimathtutor/component/**"
---

# Vaadin UI

`AGENTS.md` ("Coding Conventions" and "Critical Anti-Patterns") holds the
Vaadin rules that apply everywhere: theme-neutral styling (`--vaadin-*`
properties, `AppStyles`, neutral variants), transient injects, UI threading
through `AsyncDataLoader`, `detachEvent.getUI()`, the synchronous
`LoginView`, `CommentsPanel` and the `MathWorkspaceView` staleness checks.
This file adds how views are built here.

- **Routing:** `@Route(value = "...", layout = MainLayout.class)` for user
  views, `layout = AdminMainLayout.class` for admin views. The layouts
  enforce authentication; views add no security annotations.
- **Admin views** extend `AbstractAdminView`; a search button's call runs
  through its `loadSearch`, and a dialog's required name field comes from its
  `createNameField`.
- **User messages** go through `NotificationUtil` (`showSuccess`,
  `showError`, `showWarning`, `showInfo`), not `Notification.show`.
- **Reuse components** before adding one: `component/button/`,
  `component/dialog/` (`FormDialog`, `ConfirmationDialog`),
  `component/layout/` (search and filter layouts, `CommentsPanel`,
  `AiChatPanel`), `component/dashboard/`, `DifficultyBadge` and
  `GlassAccentBar`, and the navigation pieces (`NavigationTabs`,
  `AdminNavigationTabs`, `TopBar`). Shared styling reuses the classes named
  in `AppStyles`.
- **Admin views offer only what the caller's ceiling allows**
  (`AbstractAdminView.callerPermissions()`): rows whose rank grants beyond
  it get disabled edit/delete and no click-to-edit, rank choices beyond it
  are filtered out, and permission checkboxes the caller lacks are
  disabled. This is UX only; the services keep every check. The views gate
  on the ceiling alone, not on the caller's add, edit or delete permissions,
  which the services refuse. A save re-reads the caller's rank with
  `refreshCallerRank()` on the UI thread, since it reads the `VaadinSession`.
