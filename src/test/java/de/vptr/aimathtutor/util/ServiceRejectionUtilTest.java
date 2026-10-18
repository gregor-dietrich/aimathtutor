package de.vptr.aimathtutor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.util.Set;
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
import jakarta.validation.ConstraintDeclarationException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.UnexpectedTypeException;
import jakarta.validation.ValidationException;

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

    @Test
    @DisplayName("A broken validator setup propagates instead of showing its message")
    void validatorDefectPropagates() {
        final var defect = new UnexpectedTypeException("HV000030: No validator could be found");
        try (MockedStatic<NotificationUtil> notifications = mockStatic(NotificationUtil.class)) {
            assertEquals(defect,
                    assertThrows(UnexpectedTypeException.class, () -> ServiceRejectionUtil.runOrShowRejection(() -> {
                        throw defect;
                    })));
            notifications.verifyNoInteractions();
        }
    }

    @Test
    void testIsRejection() {
        assertTrue(ServiceRejectionUtil.isRejection(new ValidationException("Username taken")));
        assertTrue(ServiceRejectionUtil.isRejection(new ConstraintViolationException("ignored", Set.of())));
        assertFalse(ServiceRejectionUtil.isRejection(new UnexpectedTypeException("no validator")));
        assertFalse(ServiceRejectionUtil.isRejection(new ConstraintDeclarationException("bad declaration")));
    }

    @Test
    void testRejectionMessage_plainMessage() {
        assertEquals("Username taken",
                ServiceRejectionUtil.rejectionMessage(new ValidationException("Username taken")));
    }

    @Test
    void testRejectionMessage_nullMessage() {
        assertEquals("Invalid input", ServiceRejectionUtil.rejectionMessage(new ValidationException()));
    }

    @Test
    void testRejectionMessage_noViolations() {
        assertEquals("Invalid input",
                ServiceRejectionUtil.rejectionMessage(new ConstraintViolationException("ignored", Set.of())));
    }

    private static UserDto buildDto(final String username) {
        final var dto = new UserDto();
        dto.username = username;
        dto.password = "P@ssw0rd1";
        dto.rankPublicId = STUDENT_RANK_PUBLIC_ID;
        return dto;
    }
}
