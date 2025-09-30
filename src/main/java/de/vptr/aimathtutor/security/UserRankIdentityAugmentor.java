package de.vptr.aimathtutor.security;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.context.ManagedExecutor;

import de.vptr.aimathtutor.rest.entity.UserEntity;
import de.vptr.aimathtutor.rest.entity.UserRankEntity;
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

        // Page permissions
        if (Boolean.TRUE.equals(rank.pageAdd))
            roles.add("page:add");
        if (Boolean.TRUE.equals(rank.pageDelete))
            roles.add("page:delete");
        if (Boolean.TRUE.equals(rank.pageEdit))
            roles.add("page:edit");

        // Post permissions
        if (Boolean.TRUE.equals(rank.postAdd))
            roles.add("post:add");
        if (Boolean.TRUE.equals(rank.postDelete))
            roles.add("post:delete");
        if (Boolean.TRUE.equals(rank.postEdit))
            roles.add("post:edit");

        // Post category permissions
        if (Boolean.TRUE.equals(rank.postCategoryAdd))
            roles.add("post-category:add");
        if (Boolean.TRUE.equals(rank.postCategoryDelete))
            roles.add("post-category:delete");
        if (Boolean.TRUE.equals(rank.postCategoryEdit))
            roles.add("post-category:edit");

        // Post comment permissions
        if (Boolean.TRUE.equals(rank.postCommentAdd))
            roles.add("post-comment:add");
        if (Boolean.TRUE.equals(rank.postCommentDelete))
            roles.add("post-comment:delete");
        if (Boolean.TRUE.equals(rank.postCommentEdit))
            roles.add("post-comment:edit");

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

        // Account permissions
        if (Boolean.TRUE.equals(rank.userAccountAdd))
            roles.add("user-account:add");
        if (Boolean.TRUE.equals(rank.userAccountDelete))
            roles.add("user-account:delete");
        if (Boolean.TRUE.equals(rank.userAccountEdit))
            roles.add("user-account:edit");

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
