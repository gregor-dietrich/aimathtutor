package de.vptr.aimathtutor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

class AppLifecycleBeanTest {

    @Test
    @DisplayName("onStart does not throw when launch mode is TEST")
    void onStart_testMode_noException() {
        final var appLifecycleBean = new AppLifecycleBean(LaunchMode.TEST, "changeit");

        assertDoesNotThrow(() -> appLifecycleBean.onStart(new StartupEvent()));
    }

    @Test
    @DisplayName("onStart does not throw when launch mode is NORMAL but password is not default")
    void onStart_normalMode_customPassword_noException() {
        final var appLifecycleBean = new AppLifecycleBean(LaunchMode.NORMAL, "securepassword");

        assertDoesNotThrow(() -> appLifecycleBean.onStart(new StartupEvent()));
    }

    @Test
    @DisplayName("onStart throws IllegalStateException when launch mode is NORMAL and password is 'changeit'")
    void onStart_normalMode_defaultPassword_throws() {
        final var appLifecycleBean = new AppLifecycleBean(LaunchMode.NORMAL, "changeit");

        assertThrows(IllegalStateException.class, () -> appLifecycleBean.onStart(new StartupEvent()));
    }

    @Test
    @DisplayName("onStop does not throw")
    void onStop_noException() {
        final var appLifecycleBean = new AppLifecycleBean(LaunchMode.TEST, "changeit");

        assertDoesNotThrow(() -> appLifecycleBean.onStop(new ShutdownEvent()));
    }
}
