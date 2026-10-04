package de.vptr.aimathtutor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.service.UserService;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

class AppLifecycleBeanTest {

    private static final String BOOTSTRAP_PASSWORD = "B00tstr@p!";

    private final UserService userService = mock(UserService.class);

    private AppLifecycleBean bean(final LaunchMode launchMode, final String dbPassword,
            final Optional<String> bootstrapPassword) {
        return new AppLifecycleBean(launchMode, dbPassword, this.userService, "admin", bootstrapPassword);
    }

    private AppLifecycleBean productionBean(final Optional<String> bootstrapPassword) {
        return this.bean(LaunchMode.NORMAL, "securepassword", bootstrapPassword);
    }

    @Test
    @DisplayName("onStart does not throw or bootstrap when launch mode is TEST")
    void onStart_testMode_noException() {
        final var appLifecycleBean = this.bean(LaunchMode.TEST, "changeit", Optional.empty());

        assertDoesNotThrow(() -> appLifecycleBean.onStart(new StartupEvent()));
        verifyNoInteractions(this.userService);
    }

    @Test
    @DisplayName("onStart does not throw when launch mode is NORMAL but password is not default")
    void onStart_normalMode_customPassword_noException() {
        when(this.userService.hasUsers()).thenReturn(true);
        final var appLifecycleBean = this.productionBean(Optional.empty());

        assertDoesNotThrow(() -> appLifecycleBean.onStart(new StartupEvent()));
    }

    @Test
    @DisplayName("onStart throws IllegalStateException when launch mode is NORMAL and password is 'changeit'")
    void onStart_normalMode_defaultPassword_throws() {
        final var appLifecycleBean = this.bean(LaunchMode.NORMAL, "changeit", Optional.empty());

        assertThrows(IllegalStateException.class, () -> appLifecycleBean.onStart(new StartupEvent()));
    }

    @Test
    @DisplayName("onStart throws in production when no user exists and no bootstrap password is configured")
    void onStart_normalMode_noUsersNoBootstrapPassword_throws() {
        final var appLifecycleBean = this.productionBean(Optional.empty());

        assertThrows(IllegalStateException.class, () -> appLifecycleBean.onStart(new StartupEvent()));
    }

    @Test
    @DisplayName("onStart throws in production when the seeded admin password is live and no bootstrap password is set")
    void onStart_normalMode_seededAdminNoBootstrapPassword_throws() {
        when(this.userService.hasUsers()).thenReturn(true);
        when(this.userService.hasSeededAdminPassword()).thenReturn(true);
        final var appLifecycleBean = this.productionBean(Optional.empty());

        assertThrows(IllegalStateException.class, () -> appLifecycleBean.onStart(new StartupEvent()));
    }

    @Test
    @DisplayName("onStart creates the initial admin in production when no user exists")
    void onStart_normalMode_noUsers_createsInitialAdmin() {
        when(this.userService.createInitialAdmin("admin", BOOTSTRAP_PASSWORD)).thenReturn(true);
        final var appLifecycleBean = this.productionBean(Optional.of(BOOTSTRAP_PASSWORD));

        assertDoesNotThrow(() -> appLifecycleBean.onStart(new StartupEvent()));
        verify(this.userService, never()).replaceSeededAdminPassword(BOOTSTRAP_PASSWORD);
    }

    @Test
    @DisplayName("onStart replaces the seeded admin password in production when users exist")
    void onStart_normalMode_usersExist_replacesSeededAdminPassword() {
        final var appLifecycleBean = this.productionBean(Optional.of(BOOTSTRAP_PASSWORD));

        assertDoesNotThrow(() -> appLifecycleBean.onStart(new StartupEvent()));
        verify(this.userService).replaceSeededAdminPassword(BOOTSTRAP_PASSWORD);
    }

    @Test
    @DisplayName("onStop does not throw")
    void onStop_noException() {
        final var appLifecycleBean = this.bean(LaunchMode.TEST, "changeit", Optional.empty());

        assertDoesNotThrow(() -> appLifecycleBean.onStop(new ShutdownEvent()));
    }
}
