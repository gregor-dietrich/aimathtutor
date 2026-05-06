package de.vptr.aimathtutor.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.BadRequestException;

@QuarkusTest
class UlidUtilTest {

    @Test
    @DisplayName("Should generate valid ULID")
    void shouldGenerateValidUlid() {
        final String ulid = UlidUtil.generate();

        assertNotNull(ulid);
        assertFalse(ulid.isEmpty());
        assertTrue(UlidUtil.isValid(ulid));
    }

    @Test
    @DisplayName("Should generate unique ULIDs")
    void shouldGenerateUniqueUlids() {
        final String ulid1 = UlidUtil.generate();
        final String ulid2 = UlidUtil.generate();

        assertFalse(ulid1.equals(ulid2));
    }

    @Test
    @DisplayName("Should validate correct ULID format")
    void shouldValidateCorrectUlidFormat() {
        assertTrue(UlidUtil.isValid("01ARZ3NDEKTSV4RRFFQ69G5FAV"));
    }

    @Test
    @DisplayName("Should reject null ULID")
    void shouldRejectNullUlid() {
        assertFalse(UlidUtil.isValid(null));
    }

    @Test
    @DisplayName("Should reject empty ULID")
    void shouldRejectEmptyUlid() {
        assertFalse(UlidUtil.isValid(""));
    }

    @Test
    @DisplayName("Should reject ULID with invalid characters")
    void shouldRejectUlidWithInvalidCharacters() {
        assertFalse(UlidUtil.isValid("01ARZ3NDEKTSV4RRFFQ69G5FAI"));
    }

    @Test
    @DisplayName("Should reject ULID starting with invalid digit")
    void shouldRejectUlidStartingWithInvalidDigit() {
        assertFalse(UlidUtil.isValid("81ARZ3NDEKTSV4RRFFQ69G5FAV"));
    }

    @Test
    @DisplayName("Should reject ULID with wrong length")
    void shouldRejectUlidWithWrongLength() {
        assertFalse(UlidUtil.isValid("01ARZ3NDEKTSV4RRFFQ69G5FA"));
        assertFalse(UlidUtil.isValid("01ARZ3NDEKTSV4RRFFQ69G5FAVV"));
    }

    @Test
    @DisplayName("Should not throw for valid ULID")
    void shouldNotThrowForValidUlid() {
        assertDoesNotThrow(() -> UlidUtil.requireValid("01ARZ3NDEKTSV4RRFFQ69G5FAV"));
    }

    @Test
    @DisplayName("Should throw BadRequestException for invalid ULID")
    void shouldThrowBadRequestExceptionForInvalidUlid() {
        assertThrows(BadRequestException.class, () -> UlidUtil.requireValid("invalid"));
    }

    @Test
    @DisplayName("Should throw BadRequestException for null ULID")
    void shouldThrowBadRequestExceptionForNullUlid() {
        assertThrows(BadRequestException.class, () -> UlidUtil.requireValid(null));
    }
}
