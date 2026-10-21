package de.vptr.aimathtutor.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;

import de.vptr.aimathtutor.exception.PermissionDeniedException;
import de.vptr.aimathtutor.view.LoginView;
import jakarta.validation.ValidationException;

class AsyncDataLoaderTest {

    /**
     * Loads through a failing supplier and runs the UI command the loader schedules. The command runs on the test
     * thread, since a static mock only applies to the thread that created it.
     */
    private static void loadFailing(final RuntimeException failure, final UI ui) {
        final var component = mock(Component.class);
        when(component.getUI()).thenReturn(Optional.of(ui));
        final Supplier<String> supplier = () -> {
            throw failure;
        };

        AsyncDataLoader.load(supplier, component, data -> {
        }, "Load failed");

        final var command = ArgumentCaptor.forClass(Command.class);
        verify(ui, timeout(5000)).access(command.capture());
        command.getValue().execute();
    }

    @Test
    @DisplayName("A permission refusal shows its message and does not redirect")
    void permissionDeniedShowsMessage() {
        final var ui = mock(UI.class);
        try (MockedStatic<NotificationUtil> notifications = mockStatic(NotificationUtil.class);
                MockedStatic<LoginView> login = mockStatic(LoginView.class)) {
            loadFailing(new PermissionDeniedException("some message"), ui);

            notifications.verify(() -> NotificationUtil.showError("some message"));
            login.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("A rejection that the session ended shows its message and reloads onto the login page")
    void sessionEndedRedirectsToLogin() {
        final var ui = mock(UI.class);
        try (MockedStatic<NotificationUtil> notifications = mockStatic(NotificationUtil.class);
                MockedStatic<LoginView> login = mockStatic(LoginView.class)) {
            loadFailing(new ValidationException(AppConstants.SESSION_ENDED_MESSAGE), ui);

            notifications.verify(() -> NotificationUtil.showError(AppConstants.SESSION_ENDED_MESSAGE));
            login.verify(() -> LoginView.reloadToLogin(ui));
        }
    }
}
