package de.vptr.aimathtutor.security;

import java.time.LocalDateTime;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.service.LoginAttemptService;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

/**
 * Quarkus identity provider for username/password authentication.
 * Validates user credentials against the database and creates security
 * identities for authenticated users.
 */
@ApplicationScoped
public class UserIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(UserIdentityProvider.class);

    @Inject
    ManagedExecutor executor;

    @Inject
    EntityManager entityManager;

    @Inject
    PasswordHashingService passwordHashingService;

    @Inject
    LoginAttemptService loginAttemptService;

    /**
     * Returns the type of authentication request this provider handles.
     *
     * @return the class of username/password authentication requests
     */
    @Override
    public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
        return UsernamePasswordAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(final UsernamePasswordAuthenticationRequest request,
            final AuthenticationRequestContext context) {

        final String rawUsername = request.getUsername();
        final String password = new String(request.getPassword().getPassword());
        final String username = rawUsername != null ? rawUsername.toLowerCase().trim() : null;

        return Uni.createFrom().item(() -> this.authenticateUser(username, password)).runSubscriptionOn(this.executor);
    }

    @Transactional
    SecurityIdentity authenticateUser(final String username, final String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new AuthenticationFailedException("Invalid credentials");
        }

        if (this.loginAttemptService.isLockedOut(username)) {
            throw new AuthenticationFailedException("Too many failed attempts. Please try again later.");
        }

        final UserEntity user = UserEntity.find("username = ?1", username).firstResult();

        final boolean passwordValid;
        if (user == null) {
            // Perform dummy verification to maintain constant-time response
            this.passwordHashingService.verifyPassword(password,
                    "$2a$10$zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz");
            passwordValid = false;
        } else {
            passwordValid = this.passwordHashingService.verifyPassword(password, user.password);
        }

        if (!passwordValid) {
            this.loginAttemptService.recordFailedAttempt(username);
            throw new AuthenticationFailedException("Invalid credentials");
        }

        if (Boolean.TRUE.equals(user.banned)) {
            throw new AuthenticationFailedException("User is banned");
        }

        if (!Boolean.TRUE.equals(user.activated)) {
            throw new AuthenticationFailedException("User is not activated");
        }

        // Update lastLogin - ignore optimistic lock exceptions as they're not critical
        // for authentication
        try {
            this.entityManager.createQuery("UPDATE UserEntity u SET u.lastLogin = :now WHERE u.id = :id")
                    .setParameter("now", LocalDateTime.now())
                    .setParameter("id", user.id)
                    .executeUpdate();
        } catch (final PersistenceException e) {
            // Log but don't fail authentication for last_login update issues
            // (OptimisticLockException, etc.)
            LOG.debug("Failed to update last_login for user {} (this is expected during concurrent logins): {}",
                    user.username, e.getMessage());
        }

        return QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal(username))
                .build();
    }
}
