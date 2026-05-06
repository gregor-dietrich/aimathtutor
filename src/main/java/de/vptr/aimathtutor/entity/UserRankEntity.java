package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.vptr.aimathtutor.service.UlidService;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing user ranks in the system.
 */
@Entity
@Table(name = "user_ranks")
@NamedQueries({
        @NamedQuery(name = "UserRank.findAll", query = "FROM UserRankEntity ORDER BY created DESC, id DESC"),
        @NamedQuery(name = "UserRank.findByPublicId", query = "FROM UserRankEntity WHERE publicId = :p"),
        @NamedQuery(name = "UserRank.findByName", query = "FROM UserRankEntity WHERE name = :n"),
        @NamedQuery(name = "UserRank.searchByName", query = "FROM UserRankEntity WHERE LOWER(name) LIKE :s ORDER BY created DESC, id DESC")
})
public class UserRankEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Version
    public Long version;

    @Column(name = "public_id", nullable = false, unique = true, length = 26, updatable = false)
    public String publicId;

    /**
     * Generates a ULID-based public identifier for this entity if not already set.
     */
    @PrePersist
    public void generatePublicId() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UlidService.generate();
            return;
        }
        UlidService.requireValid(this.publicId);
    }

    @NotBlank
    @Column(nullable = false)
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

    @OneToMany(mappedBy = "rank")
    @JsonIgnore
    public List<UserEntity> users;

    @Generated(event = EventType.INSERT)
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    public LocalDateTime lastEdit;
}
