package de.vptr.aimathtutor.util;

import de.vptr.aimathtutor.dto.CommentDto.CommentStatus;
import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.CommentRepository;

/**
 * Factory for creating test comments in integration tests.
 */
public final class TestCommentFactory {

    private TestCommentFactory() {
    }

    /**
     * Creates a visible test comment.
     *
     * @param commentRepository
     *            the comment repository
     * @param exercise
     *            the exercise the comment belongs to
     * @param user
     *            the comment author
     * @return the persisted comment entity
     */
    public static CommentEntity createComment(final CommentRepository commentRepository, final ExerciseEntity exercise,
            final UserEntity user) {
        final var comment = new CommentEntity();
        comment.content = "Test comment";
        comment.exercise = exercise;
        comment.user = user;
        comment.status = CommentStatus.VISIBLE;
        commentRepository.persist(comment);
        return comment;
    }
}
