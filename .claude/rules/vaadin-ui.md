---
description: "Use when writing, modifying, or reviewing Vaadin views, layouts or components. Covers routing, the admin view base, async loading and the shared components. The Vaadin rules that hold everywhere stay in AGENTS.md."
paths:
  - "src/main/java/de/vptr/aimathtutor/view/**"
  - "src/main/java/de/vptr/aimathtutor/component/**"
---

# Vaadin UI

`AGENTS.md` ("Coding Conventions" and "Critical Anti-Patterns") holds the
Vaadin rules that apply everywhere: transient injects, UI threading,
`detachEvent.getUI()`, the synchronous `LoginView`, `CommentsPanel` and the
`MathWorkspaceView` staleness checks. This file adds how views are built
here.

- **Routing:** `@Route(value = "...", layout = MainLayout.class)` for user
  views, `layout = AdminMainLayout.class` for admin views. The layouts
  enforce authentication; views add no security annotations.
- **Admin views** extend `AbstractAdminView`.
- **Async loading:** use `AsyncDataLoader.load(...)` (`util/`), which wraps
  the `supplyAsync` + `ui.access` + `exceptionally` pattern with a timeout
  and error handling. Hand-roll the pattern only where the loader does not
  fit.
- **User messages** go through `NotificationUtil` (`showSuccess`,
  `showError`, `showWarning`, `showInfo`), not `Notification.show`.
- **Reuse components** before adding one: `component/button/`,
  `component/dialog/` (`BaseFormDialog`, `FormDialog`,
  `ConfirmationDialog`), `component/layout/` (search and filter layouts,
  `CommentsPanel`, `AiChatPanel`), `component/dashboard/`, and the
  navigation pieces (`NavigationTabs`, `AdminNavigationTabs`, `TopBar`).
