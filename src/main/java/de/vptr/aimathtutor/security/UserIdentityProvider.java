package de.vptr.aimathtutor.security;

import org.eclipse.microprofile.context.ManagedExecutor;

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
import jakarta.transaction.Transactional;

/**
 * Quarkus identity provider for username/password authentication.
 * Validates user credentials against the database and creates security
 * identities for authenticated users.
 */
@ApplicationScoped
public class UserIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

    // Valid 60-char bcrypt hash (cost 10) used to keep response time constant
    // when the username is not found, so attackers cannot enumerate users by timing.
    private static final String DUMMY_BCRYPT_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

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
            this.passwordHashingService.verifyPassword(password, DUMMY_BCRYPT_HASH);
            passwordValid = false;
        } else {
            passwordValid = this.passwordHashingService.verifyPassword(password, user.password);
        }

        if (!passwordValid) {
            this.loginAttemptService.recordFailedAttempt(username);
            throw new AuthenticationFailedException("Invalid credentials");
        }

        if (user == null) {
            throw new AuthenticationFailedException("User is null");
        }

        if (user.banned) {
            throw new AuthenticationFailedException("User is banned");
        }

        if (!user.activated) {
            throw new AuthenticationFailedException("User is not activated");
        }

        this.loginAttemptService.recordSuccessfulLogin(username);

        return QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal(username))
                .build();
    }
}
