package de.vptr.aimathtutor.security;

import java.util.HashSet;
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

@ApplicationScoped
public class UserRankIdentityAugmentor implements SecurityIdentityAugmentor {

    @Inject
    ManagedExecutor executor;

    @Override
    public Uni<SecurityIdentity> augment(final SecurityIdentity identity, final AuthenticationRequestContext context) {
        if (identity.isAnonymous()) {
            return Uni.createFrom().item(identity);
        }

        final var username = identity.getPrincipal().getName();

        return Uni.createFrom().item(() -> this.augmentIdentity(identity, username)).runSubscriptionOn(this.executor);
    }

    @Transactional
    SecurityIdentity augmentIdentity(final SecurityIdentity identity, final String username) {
        final UserEntity user = UserEntity.find("username", username).firstResult();

        if (user == null || user.rank == null) {
            return identity;
        }

        final Set<String> roles = this.buildRolesFromUserRank(user.rank);

        return QuarkusSecurityIdentity.builder(identity)
                .addRoles(roles)
                .build();
    }

    Set<String> buildRolesFromUserRank(final UserRankEntity rank) {
        final Set<String> roles = new HashSet<>();

        // View permissions
        if (Boolean.TRUE.equals(rank.adminView))
            roles.add("admin:view");

        // Exercise permissions
        if (Boolean.TRUE.equals(rank.exerciseAdd))
            roles.add("exercise:add");
        if (Boolean.TRUE.equals(rank.exerciseDelete))
            roles.add("exercise:delete");
        if (Boolean.TRUE.equals(rank.exerciseEdit))
            roles.add("exercise:edit");

        // Lesson permissions
        if (Boolean.TRUE.equals(rank.lessonAdd))
            roles.add("lesson:add");
        if (Boolean.TRUE.equals(rank.lessonDelete))
            roles.add("lesson:delete");
        if (Boolean.TRUE.equals(rank.lessonEdit))
            roles.add("lesson:edit");

        // Comment permissions
        if (Boolean.TRUE.equals(rank.commentAdd))
            roles.add("comment:add");
        if (Boolean.TRUE.equals(rank.commentDelete))
            roles.add("comment:delete");
        if (Boolean.TRUE.equals(rank.commentEdit))
            roles.add("comment:edit");

        // User permissions
        if (Boolean.TRUE.equals(rank.userAdd))
            roles.add("user:add");
        if (Boolean.TRUE.equals(rank.userDelete))
            roles.add("user:delete");
        if (Boolean.TRUE.equals(rank.userEdit))
            roles.add("user:edit");

        // User group permissions
        if (Boolean.TRUE.equals(rank.userGroupAdd))
            roles.add("user-group:add");
        if (Boolean.TRUE.equals(rank.userGroupDelete))
            roles.add("user-group:delete");
        if (Boolean.TRUE.equals(rank.userGroupEdit))
            roles.add("user-group:edit");

        // User rank permissions
        if (Boolean.TRUE.equals(rank.userRankAdd))
            roles.add("user-rank:add");
        if (Boolean.TRUE.equals(rank.userRankDelete))
            roles.add("user-rank:delete");
        if (Boolean.TRUE.equals(rank.userRankEdit))
            roles.add("user-rank:edit");

        return roles;
    }
}
