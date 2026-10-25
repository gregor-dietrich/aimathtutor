package de.vptr.aimathtutor.service.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import com.vaadin.flow.server.VaadinSession;

import de.vptr.aimathtutor.dto.UserDto;
import de.vptr.aimathtutor.dto.UserRankDto;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.UserRankService;
import de.vptr.aimathtutor.service.UserService;
import de.vptr.aimathtutor.util.AppConstants;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;

/**
 * A user or rank write made with a session that has ended (stale credential stamp, banned or deactivated account) is
 * refused with {@link AppConstants#SESSION_ENDED_MESSAGE}, which views turn into a return to login, and not with a
 * permission error. The real permission and authentication services run; only the Vaadin session is mocked.
 */
@QuarkusTest
class SessionEndedWriteTest {

    private static final String STUDENT_RANK_PUBLIC_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAX";
    private static final String STUDENT_PUBLIC_ID = "01ARZ3NDEKTSV4RRFFQ69G5FB3";

    @Inject
    UserService userService;

    @Inject
    UserRankService userRankService;

    @Inject
    UserRepository userRepository;

    private static UserDto validUserDto() {
        final var dto = new UserDto();
        final var suffix = UUID.randomUUID().toString().substring(0, 8);
        dto.username = "user_" + suffix;
        dto.password = "P@ssw0rd1";
        dto.email = "user_" + suffix + "@example.com";
        dto.rankPublicId = STUDENT_RANK_PUBLIC_ID;
        return dto;
    }

    private static UserRankDto validRankDto() {
        return new UserRankDto("Rank" + UUID.randomUUID().toString().substring(0, 8));
    }

    static Stream<Arguments> writes() {
        final Consumer<SessionEndedWriteTest> createUser = t -> t.userService.createUser(validUserDto());
        final Consumer<SessionEndedWriteTest> updateUser =
                t -> t.userService.updateUser(STUDENT_PUBLIC_ID, validUserDto());
        final Consumer<SessionEndedWriteTest> patchUser =
                t -> t.userService.patchUser(STUDENT_PUBLIC_ID, new UserDto());
        final Consumer<SessionEndedWriteTest> deleteUser = t -> t.userService.deleteUser(STUDENT_PUBLIC_ID);
        final Consumer<SessionEndedWriteTest> createRank = t -> t.userRankService.createRank(validRankDto());
        final Consumer<SessionEndedWriteTest> updateRank =
                t -> t.userRankService.updateRank(STUDENT_RANK_PUBLIC_ID, validRankDto());
        final Consumer<SessionEndedWriteTest> patchRank =
                t -> t.userRankService.patchRank(STUDENT_RANK_PUBLIC_ID, new UserRankDto());
        final Consumer<SessionEndedWriteTest> deleteRank = t -> t.userRankService.deleteRank(STUDENT_RANK_PUBLIC_ID);
        return Stream.of(Arguments.of("createUser", createUser), Arguments.of("updateUser", updateUser),
                Arguments.of("patchUser", patchUser), Arguments.of("deleteUser", deleteUser),
                Arguments.of("createRank", createRank), Arguments.of("updateRank", updateRank),
                Arguments.of("patchRank", patchRank), Arguments.of("deleteRank", deleteRank));
    }

    @SuppressWarnings("NullAway")
    private void runAsAdmin(final Consumer<UserEntity> change, @Nullable final String stamp,
            final Consumer<SessionEndedWriteTest> write) {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final var admin = this.userRepository.findByUsername("admin");
            change.accept(admin);
            final var session = AuthServiceTest.sessionFor(admin);
            if (stamp != null) {
                when(session.getAttribute(AppConstants.SESSION_KEY_CREDENTIAL_STAMP)).thenReturn(stamp);
            }
            mockedSession.when(VaadinSession::getCurrent).thenReturn(session);
            write.accept(this);
        }
    }

    private void assertSessionEnded(final Consumer<UserEntity> change, @Nullable final String stamp,
            final Consumer<SessionEndedWriteTest> write) {
        final var ex = assertThrows(ValidationException.class, () -> this.runAsAdmin(change, stamp, write));
        assertEquals(AppConstants.SESSION_ENDED_MESSAGE, ex.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("writes")
    @DisplayName("A write with a stale credential stamp is refused as an ended session")
    @TestTransaction
    void staleStampEndsSession(final String name, final Consumer<SessionEndedWriteTest> write) {
        this.assertSessionEnded(admin -> {
        }, "stale", write);
    }

    @Test
    @DisplayName("A write by a banned account is refused as an ended session")
    @TestTransaction
    void bannedEndsSession() {
        this.assertSessionEnded(admin -> admin.banned = true, null,
                t -> t.userService.updateUser(STUDENT_PUBLIC_ID, validUserDto()));
    }

    @Test
    @DisplayName("A write by a deactivated account is refused as an ended session")
    @TestTransaction
    void deactivatedEndsSession() {
        this.assertSessionEnded(admin -> admin.activated = false, null,
                t -> t.userService.updateUser(STUDENT_PUBLIC_ID, validUserDto()));
    }

    @Test
    @DisplayName("The same write with a valid session gets past the session check")
    @TestTransaction
    void validSessionIsAccepted() {
        assertDoesNotThrow(() -> this.runAsAdmin(admin -> {
        }, null, t -> t.userService.createUser(validUserDto())));
    }
}
