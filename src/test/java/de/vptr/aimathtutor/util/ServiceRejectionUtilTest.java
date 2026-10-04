package de.vptr.aimathtutor.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mockStatic;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.vptr.aimathtutor.dto.UserDto;
import de.vptr.aimathtutor.repository.UserRankRepository;
import de.vptr.aimathtutor.service.UserService;
import de.vptr.aimathtutor.service.security.PermissionService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class ServiceRejectionUtilTest {

    private static final String ADMIN_RANK_PUBLIC_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String STUDENT_RANK_PUBLIC_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAX";

    @Inject
    private UserService userService;

    @Inject
    private UserRankRepository userRankRepository;

    // Quarkus injects the mock, which NullAway cannot see
    @InjectMock
    @SuppressWarnings("NullAway")
    private PermissionService permissionService;

    @Test
    @DisplayName("A duplicate username shows the service's message instead of a generic error")
    @TestTransaction
    void duplicateUsernameShowsServiceMessage() {
        Mockito.doNothing().when(this.permissionService).requireUserAdd();
        Mockito.when(this.permissionService.findCurrentUserRank())
                .thenReturn(this.userRankRepository.findByPublicId(ADMIN_RANK_PUBLIC_ID).orElseThrow());
        final var username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        this.userService.createUser(buildDto(username));

        try (MockedStatic<NotificationUtil> notifications = mockStatic(NotificationUtil.class)) {
            assertFalse(ServiceRejectionUtil.runOrShowRejection(() -> this.userService.createUser(buildDto(username))));
            notifications.verify(() -> NotificationUtil.showError("Username '" + username + "' is already taken"));
            notifications.verifyNoMoreInteractions();
        }
    }

    private static UserDto buildDto(final String username) {
        final var dto = new UserDto();
        dto.username = username;
        dto.password = "P@ssw0rd1";
        dto.rankPublicId = STUDENT_RANK_PUBLIC_ID;
        return dto;
    }
}
