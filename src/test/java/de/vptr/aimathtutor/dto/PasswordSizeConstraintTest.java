package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@QuarkusTest
class PasswordSizeConstraintTest {

    private static final String PASSWORD_72 = "A1!" + "a".repeat(69);

    @Inject
    private Validator validator;

    @Test
    @DisplayName("UserDto rejects a 73-character password and names the 72-character limit")
    void userDto_rejects73Characters() {
        final var dto = new UserDto();
        dto.password = PASSWORD_72 + "a";
        final var violations = this.validator.validateProperty(dto, "password");
        assertEquals(1, violations.size());
        assertEquals("Password must be between 8 and 72 characters",
                violations.stream().map(ConstraintViolation::getMessage).findFirst().orElseThrow());
    }

    @Test
    @DisplayName("UserDto accepts a 72-character password")
    void userDto_accepts72Characters() {
        final var dto = new UserDto();
        dto.password = PASSWORD_72;
        assertTrue(this.validator.validateProperty(dto, "password").isEmpty());
    }
}
