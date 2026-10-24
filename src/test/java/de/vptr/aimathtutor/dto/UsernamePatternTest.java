package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.vptr.aimathtutor.util.AppConstants;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/** A username may contain no control (Cc) or format (Cf) character and no Unicode whitespace. */
class UsernamePatternTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest(name = "[{index}] rejected")
    @MethodSource("invisibleUsernames")
    @DisplayName("UserDto rejects a username with whitespace, control or format characters")
    void rejectsInvisibleCharacters(final String username) {
        final var dto = new UserDto();
        dto.username = username;
        final var messages =
                VALIDATOR.validateProperty(dto, "username").stream().map(ConstraintViolation::getMessage).toList();
        assertTrue(messages.contains(AppConstants.USERNAME_PATTERN_MESSAGE), messages::toString);
    }

    // The right-to-left override is built at run time: spelled out, Error Prone refuses it in source
    static Stream<String> invisibleUsernames() {
        return Stream.of("admin\u200b", Character.toString(0x202E) + "admin", "ad min", " admin", "admin ",
                "admin\u00a0", "admin\u3000", "admin\n", "admin\t", "admin\u0085");
    }

    @ParameterizedTest
    @ValueSource(strings = { "student_1", "jürgen", "admin.test" })
    @NullSource
    @DisplayName("UserDto accepts ordinary usernames and null")
    void acceptsOrdinaryUsernames(final String username) {
        final var dto = new UserDto();
        dto.username = username;
        assertEquals(0, VALIDATOR.validateProperty(dto, "username").size());
    }
}
