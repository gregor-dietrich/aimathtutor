package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.vptr.aimathtutor.util.AppConstants;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/** A username may contain only ASCII letters, digits, dot, underscore and hyphen. */
class UsernamePatternTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @ParameterizedTest(name = "[{index}] rejected")
    @MethodSource("refusedUsernames")
    @DisplayName("UserDto rejects whitespace, invisible, non-ASCII and lookalike characters")
    void rejectsCharactersOutsideTheAllowlist(final String username) {
        final var dto = new UserDto();
        dto.username = username;
        final var messages =
                VALIDATOR.validateProperty(dto, "username").stream().map(ConstraintViolation::getMessage).toList();
        assertTrue(messages.contains(AppConstants.USERNAME_PATTERN_MESSAGE), messages::toString);
    }

    // Characters that Error Prone or Checkstyle refuse spelled out or escaped in source are built at run time
    static Stream<String> refusedUsernames() {
        return Stream.of("admin\u200b", Character.toString(0x202E) + "admin", "ad min", " admin", "admin ",
                "admin\u00a0", "admin\u3000", "admin\n", "admin\t", "admin\u0085",
                "admin" + Character.toString(0xE0020), "admin\u2028", "\ufeffadmin",
                "admin" + Character.toString(0x3164), "admin" + Character.toString(0x034F),
                "admin" + Character.toString(0xFE0F), Character.toString(0x0430) + "dmin", "jürgen", "admin@example",
                "admin:1");
    }

    @ParameterizedTest
    @ValueSource(strings = { "student_1", "Admin.Test", "jane-doe" })
    @NullSource
    @DisplayName("UserDto accepts ordinary usernames and null")
    void acceptsOrdinaryUsernames(final String username) {
        final var dto = new UserDto();
        dto.username = username;
        assertEquals(0, VALIDATOR.validateProperty(dto, "username").size());
    }
}
