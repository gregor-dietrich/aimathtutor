package de.vptr.aimathtutor.security;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.eclipse.microprofile.context.ManagedExecutor;

import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Quarkus security identity augmentor that adds user ranks and associated roles
 * to authenticated users.
 * Enriches the security identity with permission-based roles derived from the
 * user's rank.
 */
@ApplicationScoped
public class UserRankIdentityAugmentor implements SecurityIdentityAugmentor {

    @Inject
    ManagedExecutor executor;

    /**
     * Augments the authenticated user's security identity with their rank and
     * associated roles/permissions.
     *
     * @param identity the current security identity
     * @param context  the authentication request context
     * @return a Uni containing the augmented security identity
     */
    @Override
    public Uni<SecurityIdentity> augment(final SecurityIdentity identity, final AuthenticationRequestContext context) {
        if (identity.isAnonymous()) {
            return Uni.createFrom().item(identity);
        }

        final var rawUsername = identity.getPrincipal().getName();
        final String username = rawUsername != null ? rawUsername.toLowerCase(Locale.ROOT).trim() : null;

        return Uni.createFrom().item(() -> this.augmentIdentity(identity, username)).runSubscriptionOn(this.executor);
    }

    @Transactional
    SecurityIdentity augmentIdentity(final SecurityIdentity identity, final String username) {
        if (username == null || username.isEmpty()) {
            return identity;
        }

        final UserEntity user = UserEntity.find("username", username).firstResult();

        if (user == null || user.rank == null) {
            return identity;
        }

        final Set<String> roles = this.buildRolesFromUserRank(user.rank);

        final Set<String> normalizedRoles = new HashSet<>();
        for (final String role : identity.getRoles()) {
            if (role != null && !role.isBlank()) {
                normalizedRoles.add(role.trim().toLowerCase(Locale.ROOT));
            }
        }
        normalizedRoles.addAll(roles);

        // Build new identity with normalized roles only.
        // builder(identity) would preserve original un-normalized roles, so we
        // copy credentials and attributes manually.
        final var builder = QuarkusSecurityIdentity.builder()
                .setPrincipal(identity.getPrincipal())
                .addRoles(normalizedRoles)
                .addPermissionChecker(identity::checkPermission);
        for (final var credential : identity.getCredentials()) {
            builder.addCredential(credential);
        }
        for (final var entry : identity.getAttributes().entrySet()) {
            builder.addAttribute(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    Set<String> buildRolesFromUserRank(final UserRankEntity rank) {
        final Set<String> roles = new HashSet<>();

        // View permissions
        if (rank.adminView) {
            roles.add("admin:view");
        }

        // Exercise permissions
        if (rank.exerciseAdd) {
            roles.add("exercise:add");
        }
        if (rank.exerciseDelete) {
            roles.add("exercise:delete");
        }
        if (rank.exerciseEdit) {
            roles.add("exercise:edit");
        }

        // Lesson permissions
        if (rank.lessonAdd) {
            roles.add("lesson:add");
        }
        if (rank.lessonDelete) {
            roles.add("lesson:delete");
        }
        if (rank.lessonEdit) {
            roles.add("lesson:edit");
        }

        // Comment permissions
        if (rank.commentAdd) {
            roles.add("comment:add");
        }
        if (rank.commentDelete) {
            roles.add("comment:delete");
        }
        if (rank.commentEdit) {
            roles.add("comment:edit");
        }

        // User permissions
        if (rank.userAdd) {
            roles.add("user:add");
        }
        if (rank.userDelete) {
            roles.add("user:delete");
        }
        if (rank.userEdit) {
            roles.add("user:edit");
        }

        // User group permissions
        if (rank.userGroupAdd) {
            roles.add("user-group:add");
        }
        if (rank.userGroupDelete) {
            roles.add("user-group:delete");
        }
        if (rank.userGroupEdit) {
            roles.add("user-group:edit");
        }

        // User rank permissions
        if (rank.userRankAdd) {
            roles.add("user-rank:add");
        }
        if (rank.userRankDelete) {
            roles.add("user-rank:delete");
        }
        if (rank.userRankEdit) {
            roles.add("user-rank:edit");
        }

        return roles;
    }
}
