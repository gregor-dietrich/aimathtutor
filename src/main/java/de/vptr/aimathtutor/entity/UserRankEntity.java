package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.vptr.aimathtutor.dto.Permission;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing user ranks in the system.
 */
@Entity
@Table(name = "user_ranks")
@NamedQueries({ @NamedQuery(name = "UserRank.findAll", query = "FROM UserRankEntity ORDER BY created DESC, id DESC"),
        @NamedQuery(name = "UserRank.findByPublicId", query = "FROM UserRankEntity WHERE publicId = :p"),
        @NamedQuery(name = "UserRank.findByName", query = "FROM UserRankEntity WHERE name = :n"),
        @NamedQuery(name = "UserRank.searchByName",
                query = "FROM UserRankEntity WHERE LOWER(name) LIKE :s ESCAPE '!' ORDER BY created DESC, id DESC") })
public class UserRankEntity extends BaseEntity {

    @NotBlank
    @Column(nullable = false)
    @Nullable
    public String name;

    // View permissions
    @Column(name = "admin_view")
    public boolean adminView = false;

    // Exercise permissions
    @Column(name = "exercise_add")
    public boolean exerciseAdd = false;

    @Column(name = "exercise_delete")
    public boolean exerciseDelete = false;

    @Column(name = "exercise_edit")
    public boolean exerciseEdit = false;

    // Lesson permissions
    @Column(name = "lesson_add")
    public boolean lessonAdd = false;

    @Column(name = "lesson_delete")
    public boolean lessonDelete = false;

    @Column(name = "lesson_edit")
    public boolean lessonEdit = false;

    // Comment permissions
    @Column(name = "comment_add")
    public boolean commentAdd = false;

    @Column(name = "comment_delete")
    public boolean commentDelete = false;

    @Column(name = "comment_edit")
    public boolean commentEdit = false;

    // User permissions
    @Column(name = "user_add")
    public boolean userAdd = false;

    @Column(name = "user_delete")
    public boolean userDelete = false;

    @Column(name = "user_edit")
    public boolean userEdit = false;

    // User group permissions
    @Column(name = "user_group_add")
    public boolean userGroupAdd = false;

    @Column(name = "user_group_delete")
    public boolean userGroupDelete = false;

    @Column(name = "user_group_edit")
    public boolean userGroupEdit = false;

    // User rank permissions
    @Column(name = "user_rank_add")
    public boolean userRankAdd = false;

    @Column(name = "user_rank_delete")
    public boolean userRankDelete = false;

    @Column(name = "user_rank_edit")
    public boolean userRankEdit = false;

    // AI configuration permissions
    @Column(name = "ai_config_edit")
    public boolean aiConfigEdit = false;

    @OneToMany(mappedBy = "rank")
    @JsonIgnore
    @Nullable
    public List<UserEntity> users;

    @Generated(event = EventType.INSERT)
    @Nullable
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    @Nullable
    public LocalDateTime lastEdit;

    /**
     * Whether this rank holds a permission. A new {@link Permission} constant must be added here (the compiler refuses
     * a missing case), together with its field and column; see {@link Permission}.
     *
     * @param permission
     *            the permission to read
     * @return true if the rank holds it
     */
    public boolean has(final Permission permission) {
        return switch (permission) {
            case ADMIN_VIEW -> this.adminView;
            case EXERCISE_ADD -> this.exerciseAdd;
            case EXERCISE_EDIT -> this.exerciseEdit;
            case EXERCISE_DELETE -> this.exerciseDelete;
            case LESSON_ADD -> this.lessonAdd;
            case LESSON_EDIT -> this.lessonEdit;
            case LESSON_DELETE -> this.lessonDelete;
            case COMMENT_ADD -> this.commentAdd;
            case COMMENT_EDIT -> this.commentEdit;
            case COMMENT_DELETE -> this.commentDelete;
            case USER_ADD -> this.userAdd;
            case USER_EDIT -> this.userEdit;
            case USER_DELETE -> this.userDelete;
            case USER_GROUP_ADD -> this.userGroupAdd;
            case USER_GROUP_EDIT -> this.userGroupEdit;
            case USER_GROUP_DELETE -> this.userGroupDelete;
            case USER_RANK_ADD -> this.userRankAdd;
            case USER_RANK_EDIT -> this.userRankEdit;
            case USER_RANK_DELETE -> this.userRankDelete;
            case AI_CONFIG_EDIT -> this.aiConfigEdit;
        };
    }

    /**
     * The permissions this rank holds, as a snapshot: later changes to the rank do not show in the returned set, so a
     * caller can capture its own permissions before editing its own rank.
     *
     * @return a new set of the held permissions
     */
    public Set<Permission> granted() {
        final var granted = EnumSet.noneOf(Permission.class);
        for (final var permission : Permission.values()) {
            if (this.has(permission)) {
                granted.add(permission);
            }
        }
        return granted;
    }

    /**
     * Whether this rank grants a permission that {@code ceiling} lacks.
     *
     * @param ceiling
     *            the permissions to compare against, e.g. another rank's {@link #granted()}
     * @return true if some permission is held here but not in {@code ceiling}
     */
    public boolean grantsBeyond(final Set<Permission> ceiling) {
        return !ceiling.containsAll(this.granted());
    }
}
