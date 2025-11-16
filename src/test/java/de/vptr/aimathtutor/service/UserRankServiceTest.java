package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.UserRankDto;
import de.vptr.aimathtutor.dto.UserRankViewDto;
import de.vptr.aimathtutor.repository.UserRankRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Integration tests for UserRankService.
 * Tests CRUD operations, searching, and permission management for user ranks.
 */
@QuarkusTest
@DisplayName("UserRankService Tests")
class UserRankServiceTest {

    @Inject
    UserRankService userRankService;

    @Inject
    UserRankRepository userRankRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up test ranks from previous runs
        final List<String> testRankNames = List.of("TestRank", "TestRankToUpdate", "TestRankToDelete",
                "TestAdminRank123", "TestUserRank456");
        for (final String name : testRankNames) {
            final var existing = this.userRankRepository.findByName(name);
            if (existing.isPresent()) {
                this.userRankRepository.deleteById(existing.get().id);
            }
        }
    }

    @Test
    @DisplayName("Create a new rank with default permissions")
    @Transactional
    void testCreateRank() {
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = "TestRank";
        rankDto.adminView = true;
        rankDto.exerciseAdd = true;
        rankDto.userEdit = false;

        final UserRankViewDto created = this.userRankService.createRank(rankDto);

        assertNotNull(created);
        assertEquals("TestRank", created.name);
        assertTrue(created.adminView);
        assertTrue(created.exerciseAdd);
        assertFalse(created.userEdit);
    }

    @Test
    @DisplayName("Retrieve rank by ID")
    @Transactional
    void testFindById() {
        // Setup: Create a rank
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = "TestRank";
        rankDto.adminView = true;
        final UserRankViewDto created = this.userRankService.createRank(rankDto);

        // Test: Find by ID
        final Optional<UserRankViewDto> found = this.userRankService.findById(created.id);

        assertTrue(found.isPresent());
        assertEquals("TestRank", found.get().name);
        assertTrue(found.get().adminView);
    }

    @Test
    @DisplayName("Find rank by name")
    @Transactional
    void testFindByName() {
        // Setup: Create a rank
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = "TestRank";
        this.userRankService.createRank(rankDto);

        // Test: Find by name
        final Optional<UserRankViewDto> found = this.userRankService.findByName("TestRank");

        assertTrue(found.isPresent());
        assertEquals("TestRank", found.get().name);
    }

    @Test
    @DisplayName("Get all ranks")
    @Transactional
    void testGetAllRanks() {
        // Setup: Create multiple ranks
        final UserRankDto rank1 = new UserRankDto();
        rank1.name = "TestRank";
        this.userRankService.createRank(rank1);

        final UserRankDto rank2 = new UserRankDto();
        rank2.name = "TestRankToUpdate";
        this.userRankService.createRank(rank2);

        // Test: Get all ranks
        final List<UserRankViewDto> allRanks = this.userRankService.getAllRanks();

        assertFalse(allRanks.isEmpty());
        assertTrue(allRanks.stream().anyMatch(r -> "TestRank".equals(r.name)));
        assertTrue(allRanks.stream().anyMatch(r -> "TestRankToUpdate".equals(r.name)));
    }

    @Test
    @DisplayName("Search ranks by query")
    @Transactional
    void testSearchRanks() {
        // Setup: Create ranks with unique names
        final UserRankDto rank1 = new UserRankDto();
        rank1.name = "TestAdminRank123";
        this.userRankService.createRank(rank1);

        final UserRankDto rank2 = new UserRankDto();
        rank2.name = "TestUserRank456";
        this.userRankService.createRank(rank2);

        // Test: Search for "admin" (should find TestAdminRank123)
        final List<UserRankViewDto> results = this.userRankService.searchRanks("admin");

        assertTrue(results.stream().anyMatch(r -> "TestAdminRank123".equals(r.name)));
    }

    @Test
    @DisplayName("Update rank permissions")
    @Transactional
    void testUpdateRank() {
        // Setup: Create a rank
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = "TestRankToUpdate";
        rankDto.adminView = false;
        rankDto.exerciseAdd = false;
        final UserRankViewDto created = this.userRankService.createRank(rankDto);

        // Test: Update the rank
        final UserRankDto updateDto = new UserRankDto();
        updateDto.name = "TestRankToUpdate";
        updateDto.adminView = true;
        updateDto.exerciseAdd = true;
        updateDto.exerciseEdit = true;
        final UserRankViewDto updated = this.userRankService.updateRank(created.id, updateDto);

        assertTrue(updated.adminView);
        assertTrue(updated.exerciseAdd);
        assertTrue(updated.exerciseEdit);
    }

    @Test
    @DisplayName("Search with empty query returns all ranks")
    @Transactional
    void testSearchWithEmptyQuery() {
        // Setup: Create a rank
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = "TestRank";
        this.userRankService.createRank(rankDto);

        // Test: Search with null/empty query should return all
        final List<UserRankViewDto> allWithNull = this.userRankService.searchRanks(null);
        final List<UserRankViewDto> allWithEmpty = this.userRankService.searchRanks("");

        assertFalse(allWithNull.isEmpty());
        assertFalse(allWithEmpty.isEmpty());
        assertTrue(allWithNull.stream().anyMatch(r -> "TestRank".equals(r.name)));
        assertTrue(allWithEmpty.stream().anyMatch(r -> "TestRank".equals(r.name)));
    }

    @Test
    @DisplayName("Update non-existent rank throws exception")
    @Transactional
    void testUpdateNonExistentRank() {
        final UserRankDto updateDto = new UserRankDto();
        updateDto.name = "NonExistent";

        assertThrows(Exception.class, () -> this.userRankService.updateRank(99999L, updateDto));
    }

    @Test
    @DisplayName("Create rank with null name throws validation exception")
    @Transactional
    void testCreateRankWithNullName() {
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = null;

        assertThrows(Exception.class, () -> this.userRankService.createRank(rankDto));
    }

    @Test
    @DisplayName("Create rank with empty name throws validation exception")
    @Transactional
    void testCreateRankWithEmptyName() {
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = "";

        assertThrows(Exception.class, () -> this.userRankService.createRank(rankDto));
    }

    @Test
    @DisplayName("Create rank with blank name throws validation exception")
    @Transactional
    void testCreateRankWithBlankName() {
        final UserRankDto rankDto = new UserRankDto();
        rankDto.name = "   ";

        assertThrows(Exception.class, () -> this.userRankService.createRank(rankDto));
    }
}
