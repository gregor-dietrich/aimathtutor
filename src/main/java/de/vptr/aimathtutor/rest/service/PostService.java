package de.vptr.aimathtutor.rest.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.rest.dto.PostDto;
import de.vptr.aimathtutor.rest.dto.PostViewDto;
import de.vptr.aimathtutor.rest.entity.PostCategoryEntity;
import de.vptr.aimathtutor.rest.entity.PostEntity;
import de.vptr.aimathtutor.rest.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class PostService {

    public List<PostViewDto> getAllPosts() {
        return PostEntity.find("ORDER BY created DESC").list().stream()
                .map(entity -> new PostViewDto((PostEntity) entity))
                .toList();
    }

    public Optional<PostViewDto> findById(final Long id) {
        return PostEntity.findByIdOptional(id)
                .map(entity -> new PostViewDto((PostEntity) entity));
    }

    public List<PostViewDto> findPublishedPosts() {
        return PostEntity.find("published = true ORDER BY created DESC").list().stream()
                .map(entity -> new PostViewDto((PostEntity) entity))
                .toList();
    }

    public List<PostViewDto> findByUserId(final Long userId) {
        return PostEntity.find("user.id = ?1 ORDER BY created DESC", userId).list().stream()
                .map(entity -> new PostViewDto((PostEntity) entity))
                .toList();
    }

    public List<PostViewDto> findByCategoryId(final Long categoryId) {
        return PostEntity.find("category.id = ?1 ORDER BY created DESC", categoryId).list().stream()
                .map(entity -> new PostViewDto((PostEntity) entity))
                .toList();
    }

    @Transactional
    public PostViewDto createPost(final PostDto postDto) {
        // Validate required fields for POST
        if (postDto.title == null || postDto.title.trim().isEmpty()) {
            throw new ValidationException("Title is required for creating a post");
        }
        if (postDto.content == null || postDto.content.trim().isEmpty()) {
            throw new ValidationException("Content is required for creating a post");
        }
        if (postDto.userId == null) {
            throw new ValidationException("User ID is required for creating a post");
        }

        final PostEntity post = new PostEntity();
        post.title = postDto.title;
        post.content = postDto.content;
        post.published = postDto.published != null ? postDto.published : false;
        post.commentable = postDto.commentable != null ? postDto.commentable : false;
        post.created = LocalDateTime.now();
        post.lastEdit = post.created;

        // Set user - required for creation
        final UserEntity user = UserEntity.findById(postDto.userId);
        if (user == null) {
            throw new ValidationException("User with ID " + postDto.userId + " not found");
        }
        post.user = user;

        // Set category if provided
        if (postDto.categoryId != null) {
            final PostCategoryEntity category = PostCategoryEntity.findById(postDto.categoryId);
            if (category == null) {
                throw new ValidationException("Category with ID " + postDto.categoryId + " not found");
            }
            post.category = category;
        }

        post.persist();
        return new PostViewDto(post);
    }

    @Transactional
    public PostViewDto updatePost(final Long id, final PostDto postDto) {
        // Validate required fields for PUT
        if (postDto.title == null || postDto.title.trim().isEmpty()) {
            throw new ValidationException("Title is required for updating a post");
        }
        if (postDto.content == null || postDto.content.trim().isEmpty()) {
            throw new ValidationException("Content is required for updating a post");
        }

        final PostEntity existingPost = PostEntity.findById(id);
        if (existingPost == null) {
            throw new WebApplicationException("Post not found", Response.Status.NOT_FOUND);
        }

        // Complete replacement (PUT semantics)
        existingPost.title = postDto.title;
        existingPost.content = postDto.content;
        existingPost.published = postDto.published != null ? postDto.published : false;
        existingPost.commentable = postDto.commentable != null ? postDto.commentable : false;
        existingPost.lastEdit = LocalDateTime.now();

        // Set user if provided, otherwise keep existing user
        if (postDto.userId != null) {
            final UserEntity user = UserEntity.findById(postDto.userId);
            if (user == null) {
                throw new ValidationException("User with ID " + postDto.userId + " not found");
            }
            existingPost.user = user;
        }
        // Note: Do not set to null if userId is not provided - preserve existing user

        // Set category if provided
        if (postDto.categoryId != null) {
            final PostCategoryEntity category = PostCategoryEntity.findById(postDto.categoryId);
            if (category == null) {
                throw new ValidationException("Category with ID " + postDto.categoryId + " not found");
            }
            existingPost.category = category;
        } else {
            existingPost.category = null;
        }

        existingPost.persist();
        return new PostViewDto(existingPost);
    }

    @Transactional
    public PostViewDto patchPost(final Long id, final PostDto postDto) {
        final PostEntity existingPost = PostEntity.findById(id);
        if (existingPost == null) {
            throw new WebApplicationException("Post not found", Response.Status.NOT_FOUND);
        }

        // Partial update (PATCH semantics) - only update provided fields
        if (postDto.title != null && !postDto.title.trim().isEmpty()) {
            existingPost.title = postDto.title;
        }
        if (postDto.content != null && !postDto.content.trim().isEmpty()) {
            existingPost.content = postDto.content;
        }
        if (postDto.published != null) {
            existingPost.published = postDto.published;
        }
        if (postDto.commentable != null) {
            existingPost.commentable = postDto.commentable;
        }

        // Set user if provided
        if (postDto.userId != null) {
            final UserEntity user = UserEntity.findById(postDto.userId);
            if (user == null) {
                throw new ValidationException("User with ID " + postDto.userId + " not found");
            }
            existingPost.user = user;
        }

        // Set category if provided
        if (postDto.categoryId != null) {
            final PostCategoryEntity category = PostCategoryEntity.findById(postDto.categoryId);
            if (category == null) {
                throw new ValidationException("Category with ID " + postDto.categoryId + " not found");
            }
            existingPost.category = category;
        }

        existingPost.lastEdit = LocalDateTime.now();
        existingPost.persist();
        return new PostViewDto(existingPost);
    }

    @Transactional
    public boolean deletePost(final Long id) {
        return PostEntity.deleteById(id);
    }

    public List<PostViewDto> searchPosts(final String query) {
        if (query == null || query.trim().isEmpty()) {
            return this.getAllPosts();
        }
        final var searchTerm = "%" + query.trim().toLowerCase() + "%";
        final List<PostEntity> posts = PostEntity.find(
                "LOWER(title) LIKE ?1 OR content LIKE ?1 OR LOWER(user.username) LIKE ?1 ORDER BY created DESC",
                searchTerm).list();
        return posts.stream()
                .map(PostViewDto::new)
                .toList();
    }

    public List<PostViewDto> findByDateRange(final String startDate, final String endDate) {
        if (startDate == null || endDate == null) {
            return this.getAllPosts();
        }

        try {
            final LocalDate start = LocalDate.parse(startDate);
            final LocalDate end = LocalDate.parse(endDate);

            final LocalDateTime startDateTime = start.atStartOfDay();
            final LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

            final List<PostEntity> posts = PostEntity
                    .find("created >= ?1 AND created <= ?2 ORDER BY created DESC", startDateTime, endDateTime).list();
            return posts.stream()
                    .map(PostViewDto::new)
                    .toList();
        } catch (final Exception e) {
            // If date parsing fails, return all posts
            return this.getAllPosts();
        }
    }

    /**
     * Alias for findPublishedPosts - for backward compatibility with views
     */
    public List<PostViewDto> getPublishedPosts() {
        return this.findPublishedPosts();
    }

    /**
     * Alias for findByDateRange - for backward compatibility with views
     */
    public List<PostViewDto> getPostsByDateRange(final String startDate, final String endDate) {
        return this.findByDateRange(startDate, endDate);
    }

    /**
     * Alias for findByUserId - for backward compatibility with views
     */
    public List<PostViewDto> getPostsByUser(final long userId) {
        return this.findByUserId(userId);
    }
}
