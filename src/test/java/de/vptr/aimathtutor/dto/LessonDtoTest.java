package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NullAway")
class LessonDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new LessonDto();
        assertNull(dto.publicId);
        assertNull(dto.name);
        assertNull(dto.parentPublicId);
        assertNull(dto.parent);
    }

    @Test
    @DisplayName("ParentField default constructor")
    void testParentFieldDefault() {
        final var field = new LessonDto.ParentField();
        assertNull(field.publicId);
    }

    @Test
    @DisplayName("ParentField parameterized constructor")
    void testParentFieldParameterized() {
        final var field = new LessonDto.ParentField("lp123");
        assertEquals("lp123", field.publicId);
    }

    @Test
    @DisplayName("syncParent copies from nested to flat")
    void testSyncParentFromNested() {
        final var dto = new LessonDto();
        dto.parent = new LessonDto.ParentField("lp1");
        dto.syncParent();
        assertEquals("lp1", dto.parentPublicId);
    }

    @Test
    @DisplayName("syncParent copies from flat to nested")
    void testSyncParentFromFlat() {
        final var dto = new LessonDto();
        dto.parentPublicId = "lp2";
        dto.syncParent();
        assertNotNull(dto.parent);
        assertEquals("lp2", dto.parent.publicId);
    }

    @Test
    @DisplayName("syncParent does nothing when both null")
    void testSyncParentBothNull() {
        final var dto = new LessonDto();
        dto.syncParent();
        assertNull(dto.parentPublicId);
        assertNull(dto.parent);
    }
}
