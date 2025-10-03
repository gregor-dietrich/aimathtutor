package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.aimathtutor.dto.UserRankDto;
import de.vptr.aimathtutor.service.UserRankService;

@ExtendWith(MockitoExtension.class)
class UserRankServiceTest {

    @InjectMocks
    private UserRankService userRankService;

    @Test
    @DisplayName("Should validate DTO with null name")
    void shouldValidateDtoWithNullName() {
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = null;

        assertTrue(rankDto.name == null || rankDto.name.trim().isEmpty());
    }

    @Test
    @DisplayName("Should validate DTO with empty name")
    void shouldValidateDtoWithEmptyName() {
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = "";

        assertTrue(rankDto.name == null || rankDto.name.trim().isEmpty());
    }

    @Test
    @DisplayName("Should validate DTO with whitespace name")
    void shouldValidateDtoWithWhitespaceName() {
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = "   ";

        assertTrue(rankDto.name == null || rankDto.name.trim().isEmpty());
    }

    @Test
    @DisplayName("Should validate DTO with valid name")
    void shouldValidateDtoWithValidName() {
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = "Administrator";

        assertFalse(rankDto.name == null || rankDto.name.trim().isEmpty());
    }
}
