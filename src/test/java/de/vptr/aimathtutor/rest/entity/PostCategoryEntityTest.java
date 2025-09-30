package de.vptr.aimathtutor.rest.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostCategoryEntityTest {

    private PostCategoryEntity category;
    private PostCategoryEntity parentCategory;

    @BeforeEach
    void setUp() {
        category = new PostCategoryEntity();
        parentCategory = new PostCategoryEntity();
        parentCategory.id = 1L;
        parentCategory.name = "Parent Category";
    }

    @Test
    @DisplayName("Should create PostCategoryEntity with all fields")
    void shouldCreatePostCategoryEntityWithAllFields() {
        // Given
        List<PostCategoryEntity> children = new ArrayList<>();
        List<PostEntity> posts = new ArrayList<>();

        // When
        category.id = 2L;
        category.name = "Child Category";
        category.parent = parentCategory;
        category.children = children;
        category.posts = posts;

        // Then
        assertEquals(2L, category.id);
        assertEquals("Child Category", category.name);
        assertEquals(parentCategory, category.parent);
        assertEquals(children, category.children);
        assertEquals(posts, category.posts);
    }

    @Test
    @DisplayName("Should identify root category correctly")
    void shouldIdentifyRootCategoryCorrectly() {
        // Given - category without parent
        category.parent = null;

        // When & Then
        assertTrue(category.isRootCategory());

        // Given - category with parent
        category.parent = parentCategory;

        // When & Then
        assertFalse(category.isRootCategory());
    }

    @Test
    @DisplayName("Should handle children collection properly")
    void shouldHandleChildrenCollectionProperly() {
        // Given
        List<PostCategoryEntity> children = new ArrayList<>();
        PostCategoryEntity child1 = new PostCategoryEntity();
        child1.id = 3L;
        child1.name = "Child 1";

        PostCategoryEntity child2 = new PostCategoryEntity();
        child2.id = 4L;
        child2.name = "Child 2";

        children.add(child1);
        children.add(child2);

        // When
        category.children = children;

        // Then
        assertNotNull(category.children);
        assertEquals(2, category.children.size());
        assertEquals(child1, category.children.get(0));
        assertEquals(child2, category.children.get(1));
    }

    @Test
    @DisplayName("Should handle posts collection properly")
    void shouldHandlePostsCollectionProperly() {
        // Given
        List<PostEntity> posts = new ArrayList<>();
        PostEntity post1 = new PostEntity();
        post1.id = 1L;
        post1.title = "Post 1";

        PostEntity post2 = new PostEntity();
        post2.id = 2L;
        post2.title = "Post 2";

        posts.add(post1);
        posts.add(post2);

        // When
        category.posts = posts;

        // Then
        assertNotNull(category.posts);
        assertEquals(2, category.posts.size());
        assertEquals(post1, category.posts.get(0));
        assertEquals(post2, category.posts.get(1));
    }

    @Test
    @DisplayName("Should handle empty collections")
    void shouldHandleEmptyCollections() {
        // When
        category.children = new ArrayList<>();
        category.posts = new ArrayList<>();

        // Then
        assertNotNull(category.children);
        assertNotNull(category.posts);
        assertTrue(category.children.isEmpty());
        assertTrue(category.posts.isEmpty());
    }

    @Test
    @DisplayName("Should handle null parent")
    void shouldHandleNullParent() {
        // When
        category.parent = null;

        // Then
        assertNull(category.parent);
        assertTrue(category.isRootCategory());
    }

    @Test
    @DisplayName("Should handle null collections")
    void shouldHandleNullCollections() {
        // When
        category.children = null;
        category.posts = null;

        // Then
        assertNull(category.children);
        assertNull(category.posts);
    }

    @Test
    @DisplayName("Should create hierarchical structure")
    void shouldCreateHierarchicalStructure() {
        // Given
        PostCategoryEntity grandparent = new PostCategoryEntity();
        grandparent.id = 1L;
        grandparent.name = "Grandparent";
        grandparent.parent = null;

        PostCategoryEntity parent = new PostCategoryEntity();
        parent.id = 2L;
        parent.name = "Parent";
        parent.parent = grandparent;

        PostCategoryEntity child = new PostCategoryEntity();
        child.id = 3L;
        child.name = "Child";
        child.parent = parent;

        // Then
        assertTrue(grandparent.isRootCategory());
        assertFalse(parent.isRootCategory());
        assertFalse(child.isRootCategory());

        assertNull(grandparent.parent);
        assertEquals(grandparent, parent.parent);
        assertEquals(parent, child.parent);
    }
}