package de.vptr.aimathtutor.component.button;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

@DisplayName("Icon-only buttons")
class IconOnlyButtonThemeTest {

    private static void assertIconTheme(final Button button) {
        assertTrue(button.getThemeNames().contains("icon"), () -> "theme names: " + button.getThemeNames());
    }

    @Test
    @DisplayName("EditButton gets the icon theme from Flow")
    void testEditButton_hasIconTheme() {
        assertIconTheme(new EditButton(e -> {
        }));
    }

    @Test
    @DisplayName("ReplyButton gets the icon theme from Flow")
    void testReplyButton_hasIconTheme() {
        assertIconTheme(new ReplyButton(e -> {
        }));
    }

    @Test
    @DisplayName("RestoreButton gets the icon theme from Flow")
    void testRestoreButton_hasIconTheme() {
        assertIconTheme(new RestoreButton(e -> {
        }));
    }

    @Test
    @DisplayName("A plain icon button gets the icon theme from Flow")
    void testIconButton_hasIconTheme() {
        assertIconTheme(new Button(new Icon(VaadinIcon.MENU)));
    }
}
