package de.vptr.aimathtutor.repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import de.vptr.aimathtutor.service.security.EncryptionService;
import de.vptr.aimathtutor.util.SearchPatternUtil;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Repository for managing user entities. Provides database access and query operations for users including find by ID,
 * username, email, and search operations.
 */
@ApplicationScoped
public class UserRepository extends AbstractRepository {

    /**
     * Counts active Administrators (alias {@code u}); the JPQL form of {@link #isActiveAdministrator(UserEntity)}.
     * Callers may append further {@code AND} conditions.
     */
    private static final String COUNT_ACTIVE_ADMINISTRATORS =
            "SELECT COUNT(u) FROM UserEntity u WHERE u.activated = true AND u.banned = false"
                    + " AND u.rank.adminView = true AND u.rank.userEdit = true AND u.rank.userRankEdit = true";

    @Inject
    EncryptionService encryptionService;

    /**
     * Retrieves a user by its unique identifier.
     *
     * @param id
     *            the user ID
     * @return the {@link UserEntity} if found, null otherwise
     */
    @Nullable
    public UserEntity findById(final Long id) {
        if (id == null) {
            return null;
        }
        return this.em.find(UserEntity.class, id);
    }

    /**
     * Retrieves an optional user by its unique identifier.
     *
     * @param id
     *            the user ID
     * @return an {@link Optional} containing the user if found, empty otherwise
     */
    public Optional<UserEntity> findByIdOptional(final Long id) {
        return Optional.ofNullable(this.findById(id));
    }

    /**
     * Retrieves a user by their public identifier.
     *
     * @param publicId
     *            the public ID of the user
     * @return an {@link Optional} containing the user if found, empty otherwise
     */
    public Optional<UserEntity> findByPublicId(final String publicId) {
        if (publicId == null) {
            return Optional.empty();
        }
        final var q = this.em.createNamedQuery("User.findByPublicId", UserEntity.class);
        q.setParameter("p", publicId);
        q.setMaxResults(1);
        return q.getResultStream().findFirst();
    }

    /**
     * Retrieves an optional user by its username.
     *
     * @param username
     *            the username to search for
     * @return an {@link Optional} containing the user if found, empty otherwise
     */
    public Optional<UserEntity> findByUsernameOptional(final String username) {
        if (username == null) {
            return Optional.empty();
        }
        final var q = this.em.createNamedQuery("User.findByUsername", UserEntity.class);
        q.setParameter("u", username);
        q.setMaxResults(1);
        return q.getResultStream().findFirst();
    }

    /**
     * Retrieves a user by its username.
     *
     * @param username
     *            the username to search for
     * @return the {@link UserEntity} if found, null otherwise
     */
    @Nullable
    public UserEntity findByUsername(final String username) {
        return this.findByUsernameOptional(username).orElse(null);
    }

    /**
     * Retrieves an optional user by email address using a blind index for equality lookup.
     *
     * @param email
     *            the plaintext email address to search for
     * @return an {@link Optional} containing the user if found, empty otherwise
     */
    public Optional<UserEntity> findByEmailOptional(final String email) {
        if (email == null) {
            return Optional.empty();
        }
        final String blindIndex = this.encryptionService.generateBlindIndex(email);
        if (blindIndex == null) {
            return Optional.empty();
        }
        final var q = this.em.createNamedQuery("User.findByEmail", UserEntity.class);
        q.setParameter("b", blindIndex);
        q.setMaxResults(1);
        return q.getResultStream().findFirst();
    }

    /**
     * Persists a user entity to the database. Always (re-)computes {@code emailBlindIndex} from the current plaintext
     * {@code email} so the blind index stays consistent with the encrypted column.
     *
     * @param user
     *            the user to persist; null values are ignored
     * @return the persisted {@link UserEntity}, or null if the input was null
     */
    @Transactional
    @Nullable
    public UserEntity persist(final UserEntity user) {
        if (user == null) {
            return null;
        }
        user.emailBlindIndex = this.encryptionService.generateBlindIndex(user.email);
        this.em.persist(user);
        return user;
    }

    /**
     * Retrieves all users ordered by creation date descending. Rank is eagerly fetched via the named query's
     * {@code JOIN FETCH} to avoid N+1 lazy loads during grid rendering.
     *
     * @return a list of {@link UserEntity} objects
     */
    public List<UserEntity> findAll() {
        final var q = this.em.createNamedQuery("User.findAllOrdered", UserEntity.class);
        return q.getResultList();
    }

    /**
     * Retrieves a paginated slice of users ordered by creation date descending. Rank is eagerly fetched.
     *
     * @param page
     *            zero-indexed page number
     * @param pageSize
     *            number of users per page
     * @return a paginated list of {@link UserEntity} objects
     */
    public List<UserEntity> findAll(final int page, final int pageSize) {
        final int sanitizedPage = Math.max(0, page);
        final int sanitizedPageSize = pageSize <= 0 ? 10 : pageSize;
        final var q = this.em.createNamedQuery("User.findAllOrdered", UserEntity.class);
        q.setFirstResult(sanitizedPage * sanitizedPageSize);
        q.setMaxResults(sanitizedPageSize);
        return q.getResultList();
    }

    /**
     * Retrieves all active users from the database.
     *
     * @return a list of active {@link UserEntity} objects
     */
    public List<UserEntity> findActiveUsers() {
        return this.listNamed("User.findActive", UserEntity.class);
    }

    /**
     * Retrieves all users with a specific rank.
     *
     * @param rankId
     *            the rank ID to filter by
     * @return a list of {@link UserEntity} objects with the specified rank
     */
    public List<UserEntity> findByRankId(final Long rankId) {
        final var q = this.em.createNamedQuery("User.findByRankId", UserEntity.class);
        q.setParameter("r", rankId);
        return q.getResultList();
    }

    /**
     * Counts users with a specific rank.
     *
     * @param rankId
     *            the rank ID to filter by
     * @return the count of users with the specified rank
     */
    public long countByRankId(final Long rankId) {
        if (rankId == null) {
            return 0L;
        }
        final var q = this.em.createNamedQuery("User.countByRankId", Long.class);
        q.setParameter("r", rankId);
        return q.getSingleResult();
    }

    /**
     * Counts users with a specific rank by public ID.
     *
     * @param rankPublicId
     *            the rank public ID to filter by
     * @return the count of users with the specified rank
     */
    public long countByRankPublicId(final String rankPublicId) {
        if (rankPublicId == null) {
            return 0L;
        }
        final var q = this.em.createQuery("SELECT COUNT(u) FROM UserEntity u WHERE u.rank.publicId = :r", Long.class);
        q.setParameter("r", rankPublicId);
        return q.getSingleResult();
    }

    /**
     * Whether a rank makes its active users Administrators: it grants {@code adminView}, {@code userEdit} and
     * {@code userRankEdit}. That set can reach the admin area, reassign any user's rank and grant any permission back,
     * so a user holding it can recover the system. The count queries below apply the same definition in JPQL.
     *
     * @param rank
     *            the rank to test, may be null
     * @return true if the rank grants all three permissions
     */
    public static boolean grantsAdministration(@Nullable final UserRankEntity rank) {
        return rank != null && rank.adminView && rank.userEdit && rank.userRankEdit;
    }

    /**
     * Whether a user is an active Administrator: activated, not banned, and holding a rank for which
     * {@link #grantsAdministration(UserRankEntity)} holds.
     *
     * @param user
     *            the user to test
     * @return true if the user is an active Administrator
     */
    public static boolean isActiveAdministrator(final UserEntity user) {
        return user.activated && !user.banned && grantsAdministration(user.rank);
    }

    /**
     * Counts active Administrators other than the given user. The excluded user's own pending changes, which Hibernate
     * flushes before running the query, therefore never affect the result.
     *
     * @param userId
     *            the ID of the user to leave out
     * @return the number of other active Administrators
     */
    public long countOtherActiveAdministrators(final long userId) {
        final var q = this.em.createQuery(COUNT_ACTIVE_ADMINISTRATORS + " AND u.id <> :id", Long.class);
        q.setParameter("id", userId);
        return q.getSingleResult();
    }

    /**
     * Counts active Administrators whose rank is not the given one.
     *
     * @param rankId
     *            the ID of the rank whose users to leave out
     * @return the number of active Administrators holding another rank
     */
    public long countActiveAdministratorsOutsideRank(final long rankId) {
        final var q = this.em.createQuery(COUNT_ACTIVE_ADMINISTRATORS + " AND u.rank.id <> :id", Long.class);
        q.setParameter("id", rankId);
        return q.getSingleResult();
    }

    /**
     * Counts all active Administrators.
     *
     * @return the number of active Administrators
     */
    public long countActiveAdministrators() {
        return this.em.createQuery(COUNT_ACTIVE_ADMINISTRATORS, Long.class).getSingleResult();
    }

    /**
     * Counts all users.
     *
     * @return total number of users
     */
    public long countAll() {
        final var q = this.em.createQuery("SELECT COUNT(u) FROM UserEntity u", Long.class);
        return q.getSingleResult();
    }

    /**
     * Searches for users matching the given search term. Takes the RAW user-entered term: terms containing {@code @}
     * are treated as an exact email lookup via the blind index (which requires the unmodified plaintext email), all
     * other terms become a case-insensitive "contains" match on the username with LIKE metacharacters escaped.
     *
     * @param searchTerm
     *            the raw search term; if null or empty, returns all users
     * @return a list of {@link UserEntity} objects matching the search term
     */
    public List<UserEntity> search(final String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return this.findAll();
        }
        final String trimmed = searchTerm.trim();
        if (trimmed.contains("@")) {
            return this.findByEmailOptional(trimmed).map(List::of).orElse(List.of());
        }
        final var pattern = SearchPatternUtil.containsPattern(trimmed.toLowerCase(Locale.ROOT));
        final var q = this.em.createNamedQuery("User.searchByTerm", UserEntity.class);
        q.setParameter("s", pattern);
        return q.getResultList();
    }

    /**
     * Deletes a user by its unique identifier.
     *
     * @param id
     *            the ID of the user to delete
     * @return true if the user was successfully deleted, false if not found
     */
    @Transactional
    public boolean deleteById(final Long id) {
        final UserEntity e = this.findById(id);
        if (e == null) {
            return false;
        }
        this.em.remove(e);
        return true;
    }

    /**
     * Deletes a user by its public identifier.
     *
     * @param publicId
     *            the public ID of the user to delete
     * @return true if the user was successfully deleted, false if not found
     */
    @Transactional
    public boolean deleteByPublicId(final String publicId) {
        final UserEntity e = this.findByPublicId(publicId).orElse(null);
        if (e == null) {
            return false;
        }
        this.em.remove(e);
        return true;
    }
}
