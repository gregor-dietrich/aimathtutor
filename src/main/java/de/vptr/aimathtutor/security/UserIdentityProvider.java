package de.vptr.aimathtutor.security;

import java.time.LocalDateTime;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.vptr.aimathtutor.entity.UserEntity;
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
 * TODO: Class documentation.
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

    /**
     * TODO: Document getRequestType().
     */
    @Override
    public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
        return UsernamePasswordAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(final UsernamePasswordAuthenticationRequest request,
            final AuthenticationRequestContext context) {

        final String username = request.getUsername();
        final String password = new String(request.getPassword().getPassword());

        return Uni.createFrom().item(() -> this.authenticateUser(username, password)).runSubscriptionOn(this.executor);
    }

    @Transactional
    SecurityIdentity authenticateUser(final String username, final String password) {
        final UserEntity user = UserEntity.find("username = ?1", username).firstResult();

        if (user == null || !this.passwordHashingService.verifyPassword(password, user.password, user.salt)) {
            throw new AuthenticationFailedException("Invalid credentials");
        }

        if (user.banned) {
            throw new AuthenticationFailedException("User is banned");
        }

        if (!user.activated) {
            throw new AuthenticationFailedException("User is not activated");
        }

        // Update lastLogin - ignore optimistic lock exceptions as they're not critical
        // for authentication
        try {
            this.entityManager.createQuery("UPDATE UserEntity u SET u.lastLogin = :now WHERE u.id = :id")
                    .setParameter("now", LocalDateTime.now())
                    .setParameter("id", user.id)
                    .executeUpdate();
        } catch (final Exception e) {
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
