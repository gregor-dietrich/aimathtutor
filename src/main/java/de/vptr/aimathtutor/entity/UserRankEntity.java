package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
     * Every permission flag of this rank, in declaration order, so two ranks' lists can be compared position by
     * position. A new permission flag must be added here, or {@link #grantsBeyond(List)} ignores it.
     *
     * @return the permission flags in a fixed order
     */
    public List<Boolean> permissions() {
        return List.of(this.adminView, this.exerciseAdd, this.exerciseDelete, this.exerciseEdit, this.lessonAdd,
                this.lessonDelete, this.lessonEdit, this.commentAdd, this.commentDelete, this.commentEdit, this.userAdd,
                this.userDelete, this.userEdit, this.userGroupAdd, this.userGroupDelete, this.userGroupEdit,
                this.userRankAdd, this.userRankDelete, this.userRankEdit, this.aiConfigEdit);
    }

    /**
     * Whether this rank grants a permission that {@code ceiling} lacks. Takes the other rank's {@link #permissions()}
     * rather than the rank itself, so a caller can capture its own permissions before editing its own rank.
     *
     * @param ceiling
     *            the permissions to compare against, as returned by {@link #permissions()}
     * @return true if some flag is set here but not in {@code ceiling}
     */
    public boolean grantsBeyond(final List<Boolean> ceiling) {
        final List<Boolean> granted = this.permissions();
        return IntStream.range(0, granted.size()).anyMatch(i -> granted.get(i) && !ceiling.get(i));
    }
}
