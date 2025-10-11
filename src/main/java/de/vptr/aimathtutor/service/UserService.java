package de.vptr.aimathtutor.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import de.vptr.aimathtutor.dto.UserDto;
import de.vptr.aimathtutor.dto.UserViewDto;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import de.vptr.aimathtutor.security.PasswordHashingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class UserService {

    @Inject
    PasswordHashingService passwordHashingService;

    @Transactional
    public List<UserViewDto> getAllUsers() {
        return UserEntity.listAll().stream()
                .map(entity -> new UserViewDto((UserEntity) entity))
                .toList();
    }

    @Transactional
    public Optional<UserViewDto> findByUsername(final String username) {
        return UserEntity.find("username", username).firstResultOptional()
                .map(entity -> new UserViewDto((UserEntity) entity));
    }

    @Transactional
    public Optional<UserViewDto> findById(final Long id) {
        return UserEntity.findByIdOptional(id)
                .map(entity -> new UserViewDto((UserEntity) entity));
    }

    @Transactional
    public Optional<UserViewDto> findByEmail(final String email) {
        return UserEntity.find("email", email).firstResultOptional()
                .map(entity -> new UserViewDto((UserEntity) entity));
    }

    /**
     * Normalizes email field by converting empty/blank strings to null.
     * This ensures that only null or valid email addresses are stored in the
     * database,
     * preventing unique constraint violations from multiple empty strings.
     */
    private String normalizeEmail(final String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return email.trim();
    }

    @Transactional
    public UserViewDto createUser(final UserDto userDto) {
        // Validate required fields for POST
        if (userDto.username == null || userDto.username.trim().isEmpty()) {
            throw new ValidationException("Username is required for creating a user");
        }
        if (userDto.password == null || userDto.password.trim().isEmpty()) {
            throw new ValidationException("Password is required for creating a user");
        }

        // Check for duplicate username
        if (this.findByUsername(userDto.username).isPresent()) {
            throw new ValidationException("Username '" + userDto.username + "' is already taken");
        }

        // Normalize email and check for duplicate email only if email is provided
        final String normalizedEmail = this.normalizeEmail(userDto.email);
        if (normalizedEmail != null && this.findByEmail(normalizedEmail).isPresent()) {
            throw new ValidationException("Email '" + normalizedEmail + "' is already in use");
        }

        final UserEntity user = new UserEntity();
        user.username = userDto.username;
        user.email = normalizedEmail;
        user.banned = userDto.banned != null ? userDto.banned : false;
        user.activated = userDto.activated != null ? userDto.activated : false;
        user.activationKey = userDto.activationKey != null ? userDto.activationKey
                : java.util.UUID.randomUUID().toString();

        // Generate salt and hash password
        final var salt = this.passwordHashingService.generateSalt();
        try {
            final var hashedPassword = this.passwordHashingService.hashPassword(userDto.password, salt);
            user.salt = salt;
            user.password = hashedPassword;
        } catch (final Exception e) {
            throw new WebApplicationException("Failed to hash password", Response.Status.INTERNAL_SERVER_ERROR);
        }

        user.created = LocalDateTime.now();
        user.lastLogin = user.created;

        // Set rank if provided, otherwise default to rank 1
        if (userDto.rankId != null) {
            final UserRankEntity rank = UserRankEntity.findById(userDto.rankId);
            if (rank == null) {
                throw new ValidationException("Rank with ID " + userDto.rankId + " not found");
            }
            user.rank = rank;
        } else {
            user.rank = UserRankEntity.findById(1L);
        }

        user.persist();
        return new UserViewDto(user);
    }

    @Transactional
    public UserViewDto updateUser(final Long id, final UserDto userDto) {
        // Validate required fields for PUT
        if (userDto.username == null || userDto.username.trim().isEmpty()) {
            throw new ValidationException("Username is required for updating a user");
        }

        final UserEntity existingUser = UserEntity.findById(id);
        if (existingUser == null) {
            throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
        }

        // Check for duplicate username (only if username is different from current)
        if (!userDto.username.equals(existingUser.username)) {
            if (this.findByUsername(userDto.username).isPresent()) {
                throw new ValidationException("Username '" + userDto.username + "' is already taken");
            }
        }

        // Normalize email and check for duplicate email (only if email is different
        // from current)
        final String normalizedEmail = this.normalizeEmail(userDto.email);
        if (!Objects.equals(normalizedEmail, existingUser.email)) {
            if (normalizedEmail != null && this.findByEmail(normalizedEmail).isPresent()) {
                throw new ValidationException("Email '" + normalizedEmail + "' is already in use");
            }
        }

        // Complete replacement (PUT semantics)
        existingUser.username = userDto.username;
        existingUser.email = normalizedEmail;
        existingUser.banned = userDto.banned != null ? userDto.banned : false;
        existingUser.activated = userDto.activated != null ? userDto.activated : false;
        existingUser.activationKey = userDto.activationKey;

        // Handle password update if provided
        if (userDto.password != null && !userDto.password.trim().isEmpty()) {
            final var salt = this.passwordHashingService.generateSalt();
            try {
                final var hashedPassword = this.passwordHashingService.hashPassword(userDto.password, salt);
                existingUser.salt = salt;
                existingUser.password = hashedPassword;
            } catch (final Exception e) {
                throw new WebApplicationException("Failed to hash password", Response.Status.INTERNAL_SERVER_ERROR);
            }
        }

        // Set rank if provided
        if (userDto.rankId != null) {
            final UserRankEntity rank = UserRankEntity.findById(userDto.rankId);
            if (rank == null) {
                throw new ValidationException("Rank with ID " + userDto.rankId + " not found");
            }
            existingUser.rank = rank;
        } else {
            // For PUT, null rank should reset to default
            existingUser.rank = UserRankEntity.findById(1L);
        }

        existingUser.persist();
        return new UserViewDto(existingUser);
    }

    @Transactional
    public UserViewDto patchUser(final Long id, final UserDto userDto) {
        final UserEntity existingUser = UserEntity.findById(id);
        if (existingUser == null) {
            throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
        }

        // Check for duplicate username if username is being updated
        if (userDto.username != null && !userDto.username.trim().isEmpty()
                && !userDto.username.equals(existingUser.username)) {
            if (this.findByUsername(userDto.username).isPresent()) {
                throw new ValidationException("Username '" + userDto.username + "' is already taken");
            }
        }

        // Check for duplicate email if email is being updated
        if (userDto.email != null) {
            final String normalizedEmail = this.normalizeEmail(userDto.email);
            if (!Objects.equals(normalizedEmail, existingUser.email)) {
                if (normalizedEmail != null && this.findByEmail(normalizedEmail).isPresent()) {
                    throw new ValidationException("Email '" + normalizedEmail + "' is already in use");
                }
            }
        }

        // Partial update (PATCH semantics) - only update provided fields
        if (userDto.username != null && !userDto.username.trim().isEmpty()) {
            existingUser.username = userDto.username;
        }
        if (userDto.email != null) {
            existingUser.email = this.normalizeEmail(userDto.email);
        }
        if (userDto.banned != null) {
            existingUser.banned = userDto.banned;
        }
        if (userDto.activated != null) {
            existingUser.activated = userDto.activated;
        }
        if (userDto.activationKey != null) {
            existingUser.activationKey = userDto.activationKey;
        }

        // Handle password update if provided
        if (userDto.password != null && !userDto.password.trim().isEmpty()) {
            final var salt = this.passwordHashingService.generateSalt();
            try {
                final var hashedPassword = this.passwordHashingService.hashPassword(userDto.password, salt);
                existingUser.salt = salt;
                existingUser.password = hashedPassword;
            } catch (final Exception e) {
                throw new WebApplicationException("Failed to hash password", Response.Status.INTERNAL_SERVER_ERROR);
            }
        }

        // Set rank if provided
        if (userDto.rankId != null) {
            final UserRankEntity rank = UserRankEntity.findById(userDto.rankId);
            if (rank == null) {
                throw new ValidationException("Rank with ID " + userDto.rankId + " not found");
            }
            existingUser.rank = rank;
        }

        existingUser.persist();
        return new UserViewDto(existingUser);
    }

    @Transactional
    public boolean deleteUser(final Long id) {
        return UserEntity.deleteById(id);
    }

    public List<UserViewDto> findActiveUsers() {
        return UserEntity.find("activated = true and banned = false").list().stream()
                .map(entity -> new UserViewDto((UserEntity) entity))
                .toList();
    }

    public List<UserViewDto> searchUsers(final String query) {
        if (query == null || query.trim().isEmpty()) {
            return this.getAllUsers();
        }
        final var searchTerm = "%" + query.trim().toLowerCase() + "%";
        final List<UserEntity> users = UserEntity.find(
                "LOWER(username) LIKE ?1 OR LOWER(email) LIKE ?1",
                searchTerm).list();
        return users.stream()
                .map(UserViewDto::new)
                .toList();
    }

    /**
     * Get current user from session
     */
    public UserViewDto getCurrentUser() {
        final var session = com.vaadin.flow.server.VaadinSession.getCurrent();
        if (session == null) {
            throw new WebApplicationException("No active session", Response.Status.UNAUTHORIZED);
        }
        final var username = (String) session.getAttribute("authenticated.username");
        if (username == null) {
            throw new WebApplicationException("User not authenticated", Response.Status.UNAUTHORIZED);
        }
        return this.findByUsername(username)
                .orElseThrow(() -> new WebApplicationException("User not found", Response.Status.NOT_FOUND));
    }
}