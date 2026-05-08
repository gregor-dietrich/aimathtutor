package de.vptr.aimathtutor.repository;

import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import jakarta.inject.Inject;

/**
 * Base class for repository integration tests providing shared test data helpers.
 */
public abstract class AbstractRepositoryIT {

    @Inject
    protected UserRepository userRepository;

    @Inject
    protected ExerciseRepository exerciseRepository;

    @Inject
    protected UserRankRepository userRankRepository;

    protected UserEntity createUser(final String suffix, final String usernamePrefix) {
        final UserRankEntity rank = new UserRankEntity();
        rank.name = "Rank_" + suffix;
        this.userRankRepository.persist(rank);

        final UserEntity user = new UserEntity();
        user.username = usernamePrefix + suffix;
        user.password = "pw";
        user.email = usernamePrefix + suffix + "@example.com";
        user.activated = true;
        user.rank = rank;
        this.userRepository.persist(user);
        return user;
    }

    protected ExerciseEntity createExercise(final UserEntity user, final String title, final String content) {
        final ExerciseEntity ex = new ExerciseEntity();
        ex.title = title;
        ex.content = content;
        ex.user = user;
        ex.published = true;
        this.exerciseRepository.persist(ex);
        return ex;
    }
}
