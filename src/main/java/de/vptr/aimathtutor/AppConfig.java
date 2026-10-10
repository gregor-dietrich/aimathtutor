package de.vptr.aimathtutor;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * Application shell configuration (theme, page title and push settings) for the Vaadin application.
 */
@StyleSheet("/" + Lumo.STYLESHEET)
@StyleSheet("/styles.css")
@PageTitle("AI Math Tutor")
@Push
// Security pin: @vaadin/markdown pulls in dompurify, and with package-lock.json missing (as after
// scripts/regen-frontend.sh) Vaadin reseeds its own tested lock, resetting a lockfile-only bump.
// Declaring it here puts it in package.json, which regeneration preserves. 3.4.16 fixes
// GHSA-p98j-92pf-mc4p; devkit's check_frontend_deps.py enforces the minimum. Remove once Vaadin
// ships >= 3.4.16 itself.
@NpmPackage(value = "dompurify", version = "3.4.16")
// Security pin: Vite's postcss pulls in source-map-js, which Vaadin's tested lock pins at 1.2.1. Pinned
// here for the same reason as dompurify; it is build-only, and nothing imports it, so declaring it here
// doesn't bundle it. 1.2.2 fixes GHSA-68fv-2mgg-jv7q; devkit's check_frontend_deps.py enforces the minimum.
// Remove once Vaadin ships >= 1.2.2 itself.
@NpmPackage(value = "source-map-js", version = "1.2.2")
public class AppConfig implements AppShellConfigurator {
}
