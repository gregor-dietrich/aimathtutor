package de.vptr.aimathtutor.util;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.net.URLClassLoader;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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

    @Test
    @DisplayName("An unexpected exception shows the plain error message, not its text")
    void unexpectedExceptionShowsPlainMessage() {
        final var ui = mock(UI.class);
        try (MockedStatic<NotificationUtil> notifications = mockStatic(NotificationUtil.class);
                MockedStatic<LoginView> login = mockStatic(LoginView.class)) {
            loadFailing(new IllegalStateException("internal detail"), ui);

            notifications.verify(() -> NotificationUtil.showError("Load failed"));
            login.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("A timeout tells the user to reload and still runs the error callback")
    void timeoutShowsReloadHintAndRunsOnError() throws InterruptedException {
        final var ui = mock(UI.class);
        final var component = mock(Component.class);
        when(component.getUI()).thenReturn(Optional.of(ui));
        final var release = new CountDownLatch(1);
        final var errorRan = new AtomicBoolean();
        try (MockedStatic<NotificationUtil> notifications = mockStatic(NotificationUtil.class)) {
            try {
                AsyncDataLoader.<String>load(() -> {
                    try {
                        release.await();
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return "late";
                }, component, data -> {
                }, () -> errorRan.set(true), Duration.ofMillis(50), "Load failed");

                final var command = ArgumentCaptor.forClass(Command.class);
                verify(ui, timeout(5000)).access(command.capture());
                command.getValue().execute();
            } finally {
                release.countDown();
            }

            notifications.verify(() -> NotificationUtil.showError(
                    "Load failed. The server did not respond in time; reload the page to see the current state."));
            assertTrue(errorRan.get());
        }
    }

    @Test
    @DisplayName("The UI callback runs under the caller's context class loader")
    void callbackRunsUnderCallersClassLoader() throws Exception {
        final var ui = mock(UI.class);
        final var component = mock(Component.class);
        when(component.getUI()).thenReturn(Optional.of(ui));
        final var seen = new AtomicReference<ClassLoader>();
        when(ui.access(any())).thenAnswer(invocation -> {
            seen.set(Thread.currentThread().getContextClassLoader());
            return null;
        });
        final var thread = Thread.currentThread();
        final var original = thread.getContextClassLoader();
        try (var marker = new URLClassLoader(new URL[0], original)) {
            thread.setContextClassLoader(marker);
            try {
                AsyncDataLoader.load(() -> "data", component, data -> {
                }, "Load failed");
                verify(ui, timeout(5000)).access(any());
            } finally {
                thread.setContextClassLoader(original);
            }
            assertSame(marker, seen.get());
        }
    }
}
