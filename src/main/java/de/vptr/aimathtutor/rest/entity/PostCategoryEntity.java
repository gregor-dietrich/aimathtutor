package de.vptr.aimathtutor.rest.entity;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "post_categories")
public class PostCategoryEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    public String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    public PostCategoryEntity parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    public List<PostCategoryEntity> children;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    public List<PostEntity> posts;

    // Helper method to check if this is a root category
    public boolean isRootCategory() {
        return this.parent == null;
    }

    // Helper method to get all subcategories recursively
    public static List<PostCategoryEntity> findByParentId(final Long parentId) {
        if (parentId == null) {
            return find("parent IS NULL").list();
        }
        return find("parent.id", parentId).list();
    }

    // Helper method to find root categories
    public static List<PostCategoryEntity> findRootCategories() {
        return find("parent IS NULL").list();
    }
}
