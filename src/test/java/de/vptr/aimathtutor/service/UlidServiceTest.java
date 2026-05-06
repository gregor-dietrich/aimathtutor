package de.vptr.aimathtutor.service;

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
class UlidServiceTest {

    @Test
    @DisplayName("Should generate valid ULID")
    void shouldGenerateValidUlid() {
        final String ulid = UlidService.generate();

        assertNotNull(ulid);
        assertFalse(ulid.isEmpty());
        assertTrue(UlidService.isValid(ulid));
    }

    @Test
    @DisplayName("Should generate unique ULIDs")
    void shouldGenerateUniqueUlids() {
        final String ulid1 = UlidService.generate();
        final String ulid2 = UlidService.generate();

        assertFalse(ulid1.equals(ulid2));
    }

    @Test
    @DisplayName("Should validate correct ULID format")
    void shouldValidateCorrectUlidFormat() {
        assertTrue(UlidService.isValid("01ARZ3NDEKTSV4RRFFQ69G5FAV"));
    }

    @Test
    @DisplayName("Should reject null ULID")
    void shouldRejectNullUlid() {
        assertFalse(UlidService.isValid(null));
    }

    @Test
    @DisplayName("Should reject empty ULID")
    void shouldRejectEmptyUlid() {
        assertFalse(UlidService.isValid(""));
    }

    @Test
    @DisplayName("Should reject ULID with invalid characters")
    void shouldRejectUlidWithInvalidCharacters() {
        assertFalse(UlidService.isValid("01ARZ3NDEKTSV4RRFFQ69G5FAI"));
    }

    @Test
    @DisplayName("Should reject ULID starting with invalid digit")
    void shouldRejectUlidStartingWithInvalidDigit() {
        assertFalse(UlidService.isValid("81ARZ3NDEKTSV4RRFFQ69G5FAV"));
    }

    @Test
    @DisplayName("Should reject ULID with wrong length")
    void shouldRejectUlidWithWrongLength() {
        assertFalse(UlidService.isValid("01ARZ3NDEKTSV4RRFFQ69G5FA"));
        assertFalse(UlidService.isValid("01ARZ3NDEKTSV4RRFFQ69G5FAVV"));
    }

    @Test
    @DisplayName("Should not throw for valid ULID")
    void shouldNotThrowForValidUlid() {
        assertDoesNotThrow(() -> UlidService.requireValid("01ARZ3NDEKTSV4RRFFQ69G5FAV"));
    }

    @Test
    @DisplayName("Should throw BadRequestException for invalid ULID")
    void shouldThrowBadRequestExceptionForInvalidUlid() {
        assertThrows(BadRequestException.class, () -> UlidService.requireValid("invalid"));
    }

    @Test
    @DisplayName("Should throw BadRequestException for null ULID")
    void shouldThrowBadRequestExceptionForNullUlid() {
        assertThrows(BadRequestException.class, () -> UlidService.requireValid(null));
    }
}
