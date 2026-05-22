package de.vptr.aimathtutor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

class AppLifecycleBeanTest {

    private AppLifecycleBean appLifecycleBean;

    @BeforeEach
    void setUp() {
        this.appLifecycleBean = new AppLifecycleBean();
    }

    @Test
    @DisplayName("onStart does not throw when launch mode is TEST")
    void onStart_testMode_noException() {
        this.appLifecycleBean.launchMode = LaunchMode.TEST;
        this.appLifecycleBean.dbPassword = "changeit";

        assertDoesNotThrow(() -> this.appLifecycleBean.onStart(new StartupEvent()));
    }

    @Test
    @DisplayName("onStart does not throw when launch mode is NORMAL but password is not default")
    void onStart_normalMode_customPassword_noException() {
        this.appLifecycleBean.launchMode = LaunchMode.NORMAL;
        this.appLifecycleBean.dbPassword = "securepassword";

        assertDoesNotThrow(() -> this.appLifecycleBean.onStart(new StartupEvent()));
    }

    @Test
    @DisplayName("onStart throws IllegalStateException when launch mode is NORMAL and password is 'changeit'")
    void onStart_normalMode_defaultPassword_throws() {
        this.appLifecycleBean.launchMode = LaunchMode.NORMAL;
        this.appLifecycleBean.dbPassword = "changeit";

        assertThrows(IllegalStateException.class, () -> this.appLifecycleBean.onStart(new StartupEvent()));
    }

    @Test
    @DisplayName("onStop does not throw")
    void onStop_noException() {
        assertDoesNotThrow(() -> this.appLifecycleBean.onStop(new ShutdownEvent()));
    }
}
