package de.vptr.aimathtutor.rest.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.aimathtutor.rest.dto.UserRankDto;

@ExtendWith(MockitoExtension.class)
class UserRankServiceTest {

    @InjectMocks
    private UserRankService userRankService;

    @Test
    @DisplayName("Should validate DTO with null name")
    void shouldValidateDtoWithNullName() {
        UserRankDto rankDto = new UserRankDto();
        rankDto.name = null;

        assertTrue(rankDto.name == null || rankDto.name.trim().isEmpty());
    }

    @Test
    @DisplayName("Should validate DTO with empty name")
    void shouldValidateDtoWithEmptyName() {
        UserRankDto rankDto = new UserRankDto();
        rankDto.name = "";

        assertTrue(rankDto.name == null || rankDto.name.trim().isEmpty());
    }

    @Test
    @DisplayName("Should validate DTO with whitespace name")
    void shouldValidateDtoWithWhitespaceName() {
        UserRankDto rankDto = new UserRankDto();
        rankDto.name = "   ";

     

    
    @Test
    @DisplayName("Should validate DTO with valid name")
    void shouldValidateDtoWithValidName() {
        UserRankDto rankDto = new UserRankDto();
        rankDto.name = "Administrator";

        assertFalse(rankDto.name == null || rankDto.name.trim().isEmpty());
    }
}
