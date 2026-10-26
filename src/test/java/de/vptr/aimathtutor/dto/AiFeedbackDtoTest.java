package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiFeedbackDtoTest {

    @Test
    @DisplayName("clampConfidence drops a NaN confidence")
    void clampConfidenceDropsNaN() {
        final var dto = new AiFeedbackDto();
        dto.confidence = Double.NaN;

        dto.clampConfidence();

        assertNull(dto.confidence);
    }

    @Test
    @DisplayName("clampConfidence keeps an in-range confidence and clamps an out-of-range one to [0.0, 1.0]")
    void clampConfidenceClampsToRange() {
        final var dto = new AiFeedbackDto();

        dto.confidence = 0.4;
        dto.clampConfidence();
        assertEquals(0.4, dto.confidence);

        dto.confidence = 7.0;
        dto.clampConfidence();
        assertEquals(1.0, dto.confidence);

        dto.confidence = -3.0;
        dto.clampConfidence();
        assertEquals(0.0, dto.confidence);
    }
}
