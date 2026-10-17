package de.vptr.aimathtutor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

@DisplayName("NotificationUtil")
class NotificationUtilTest {

    private static final String MESSAGE = "Hello";

    static Stream<Arguments> shortcuts() {
        return Stream.of(
                Arguments.of("success", (Consumer<String>) NotificationUtil::showSuccess, NotificationVariant.SUCCESS,
                        AppConstants.NOTIFICATION_DURATION_SUCCESS_MS),
                Arguments.of("error", (Consumer<String>) NotificationUtil::showError, NotificationVariant.ERROR,
                        AppConstants.NOTIFICATION_DURATION_ERROR_MS),
                Arguments.of("warning", (Consumer<String>) NotificationUtil::showWarning, NotificationVariant.WARNING,
                        AppConstants.NOTIFICATION_DURATION_WARNING_MS),
                Arguments.of("info", (Consumer<String>) NotificationUtil::showInfo, NotificationVariant.INFO,
                        AppConstants.NOTIFICATION_DURATION_INFO_MS));
    }

    @ParameterizedTest(name = "show {0} opens a top-centre notification with its variant and duration")
    @MethodSource("shortcuts")
    @DisplayName("Should configure and open one notification per shortcut")
    void testShortcut(final String name, final Consumer<String> shortcut, final NotificationVariant variant,
            final int duration) {
        try (final MockedConstruction<Notification> constructed = Mockito.mockConstruction(Notification.class)) {
            shortcut.accept(MESSAGE);

            assertEquals(1, constructed.constructed().size());
            final var notification = constructed.constructed().get(0);
            verify(notification).addThemeVariants(variant);
            verify(notification).setText(MESSAGE);
            verify(notification).setDuration(duration);
            verify(notification).setPosition(Notification.Position.TOP_CENTER);
            verify(notification).open();
        }
    }

    @Test
    @DisplayName("Should use the given position in the four-argument overload")
    void testShowNotification_customPosition() {
        try (final MockedConstruction<Notification> constructed = Mockito.mockConstruction(Notification.class)) {
            NotificationUtil.showNotification(MESSAGE, NotificationVariant.INFO, 1234,
                    Notification.Position.BOTTOM_END);

            assertEquals(1, constructed.constructed().size());
            final var notification = constructed.constructed().get(0);
            verify(notification).addThemeVariants(NotificationVariant.INFO);
            verify(notification).setText(MESSAGE);
            verify(notification).setDuration(1234);
            verify(notification).setPosition(Notification.Position.BOTTOM_END);
            verify(notification).open();
        }
    }
}
