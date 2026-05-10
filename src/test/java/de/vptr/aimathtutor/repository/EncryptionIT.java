package de.vptr.aimathtutor.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import de.vptr.aimathtutor.service.security.EncryptionService;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/**
 * Integration tests verifying that PII fields are stored as ciphertext in the database and that blind-index lookups
 * return the correct decrypted entity.
 */
@QuarkusTest
public class EncryptionIT {

    @Inject
    UserRepository userRepository;

    @Inject
    UserRankRepository userRankRepository;

    @Inject
    EncryptionService encryptionService;

    @Inject
    DataSource dataSource;

    private UserEntity createUser(final String suffix, @Nullable final String email) {
        final UserRankEntity rank = new UserRankEntity();
        rank.name = "Rank_enc_" + suffix;
        this.userRankRepository.persist(rank);

        final UserEntity user = new UserEntity();
        user.username = "enc_" + suffix;
        user.password = "pw_" + suffix;
        user.email = email;
        user.activated = true;
        user.rank = rank;
        this.userRepository.persist(user);
        return user;
    }

    @Test
    @TestTransaction
    public void testEmailStoredAsCiphertextNotPlaintext() throws SQLException {
        final String email = "enc_test_" + UUID.randomUUID() + "@example.com";
        final UserEntity user = this.createUser(UUID.randomUUID().toString().substring(0, 8), email);

        final String dbEmail = readRawEmailFromDb(Objects.requireNonNull(user.username));
        Assertions.assertNotNull(dbEmail, "Email column must not be null after persist");
        Assertions.assertNotEquals(email, dbEmail, "Plaintext must not be stored in the email column");
        Assertions.assertTrue(dbEmail.startsWith("1|"), "Stored value must be a versioned encryption envelope");
    }

    @Test
    @TestTransaction
    public void testFindByEmailReturnsDecryptedUser() {
        final String email = "find_" + UUID.randomUUID() + "@example.com";
        final UserEntity created = this.createUser(UUID.randomUUID().toString().substring(0, 8), email);

        final Optional<UserEntity> found = this.userRepository.findByEmailOptional(email);
        Assertions.assertTrue(found.isPresent(), "findByEmailOptional must find the user via blind index");
        Assertions.assertEquals(created.username, found.get().username);
        Assertions.assertEquals(email, found.get().email, "Decrypted email must match original plaintext");
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    public void testFindByEmailNullReturnsEmpty() {
        Assertions.assertTrue(this.userRepository.findByEmailOptional(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testBlindIndexStoredAndConsistent() throws SQLException {
        final String email = "idx_" + UUID.randomUUID() + "@example.com";
        final UserEntity user = this.createUser(UUID.randomUUID().toString().substring(0, 8), email);

        final String expectedIndex = this.encryptionService.generateBlindIndex(email);
        final String dbIndex = readRawBlindIndexFromDb(Objects.requireNonNull(user.username));
        Assertions.assertEquals(expectedIndex, dbIndex,
                "Stored blind index must match blind index computed from the original email");
    }

    @Test
    @TestTransaction
    public void testNullEmailIsStoredAsNull() throws SQLException {
        final UserEntity user = this.createUser(UUID.randomUUID().toString().substring(0, 8), null);

        final String dbEmail = readRawEmailFromDb(Objects.requireNonNull(user.username));
        final String dbIndex = readRawBlindIndexFromDb(Objects.requireNonNull(user.username));
        Assertions.assertNull(dbEmail, "Null email must be stored as NULL in the DB");
        Assertions.assertNull(dbIndex, "Null email must produce NULL blind index");
    }

    @Nullable
    private String readRawEmailFromDb(final String username) throws SQLException {
        try (Connection c = this.dataSource.getConnection();
                PreparedStatement ps = c.prepareStatement("SELECT email FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    @Nullable
    private String readRawBlindIndexFromDb(final String username) throws SQLException {
        try (Connection c = this.dataSource.getConnection();
                PreparedStatement ps = c.prepareStatement("SELECT email_blind_index FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
