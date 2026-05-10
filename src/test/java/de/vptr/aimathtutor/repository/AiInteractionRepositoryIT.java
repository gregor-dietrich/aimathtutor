package de.vptr.aimathtutor.repository;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.AiInteractionEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Integration tests for {@link AiInteractionRepository}.
 */
@QuarkusTest
@SuppressWarnings("NullAway")
public class AiInteractionRepositoryIT extends AbstractRepositoryIT {

    @Inject
    AiInteractionRepository aiInteractionRepository;

    private AiInteractionEntity createInteraction(final String sessionId, final String suffix) {
        final AiInteractionEntity interaction = new AiInteractionEntity();
        interaction.sessionId = sessionId;
        interaction.eventType = "simplify_" + suffix;
        interaction.feedbackType = "POSITIVE";
        interaction.actionCorrect = true;
        this.aiInteractionRepository.persist(interaction);
        return interaction;
    }

    private AiInteractionEntity createInteractionWithUser(final String sessionId, final String suffix) {
        final UserEntity user = this.createUser(suffix, "aiuser_");
        final ExerciseEntity ex = this.createExercise(user, "Exercise_" + suffix, "x + 1");

        final AiInteractionEntity interaction = new AiInteractionEntity();
        interaction.sessionId = sessionId;
        interaction.eventType = "factor_" + suffix;
        interaction.feedbackType = "CORRECTIVE";
        interaction.user = user;
        interaction.exercise = ex;
        interaction.actionCorrect = false;
        this.aiInteractionRepository.persist(interaction);
        return interaction;
    }

    @Test
    @TestTransaction
    public void testFindAll_returnsInteractions() {
        this.createInteraction("sess-fall", "fall");
        final List<AiInteractionEntity> all = this.aiInteractionRepository.findAll();
        Assertions.assertFalse(all.isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByPublicId_found() {
        final AiInteractionEntity interaction = this.createInteraction("sess-fpub", "fpub");
        final var found = this.aiInteractionRepository.findByPublicId(Objects.requireNonNull(interaction.publicId));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(interaction.publicId, found.get().publicId);
    }

    @Test
    @TestTransaction
    public void testFindByPublicId_notFound() {
        Assertions.assertTrue(this.aiInteractionRepository.findByPublicId("nonexistent-public-id").isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByPublicId_null() {
        Assertions.assertTrue(this.aiInteractionRepository.findByPublicId(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindBySessionId_found() {
        final String sessionId = "sess-found-aiit";
        this.createInteraction(sessionId, "fsess");
        final List<AiInteractionEntity> result = this.aiInteractionRepository.findBySessionId(sessionId);
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertTrue(result.stream().anyMatch(r -> sessionId.equals(r.sessionId)));
    }

    @Test
    @TestTransaction
    public void testFindBySessionId_null() {
        final List<AiInteractionEntity> result = this.aiInteractionRepository.findBySessionId(null);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByUserId_found() {
        final AiInteractionEntity interaction = this.createInteractionWithUser("sess-uid", "fuid");
        final Long userId = Objects.requireNonNull(Objects.requireNonNull(interaction.user).id);
        final List<AiInteractionEntity> result = this.aiInteractionRepository.findByUserId(userId);
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertTrue(result.stream().anyMatch(r -> userId.equals(Objects.requireNonNull(r.user).id)));
    }

    @Test
    @TestTransaction
    public void testFindByUserId_null() {
        final List<AiInteractionEntity> result = this.aiInteractionRepository.findByUserId(null);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByExerciseId_found() {
        final AiInteractionEntity interaction = this.createInteractionWithUser("sess-exid", "fexid");
        final Long exId = Objects.requireNonNull(Objects.requireNonNull(interaction.exercise).id);
        final List<AiInteractionEntity> result = this.aiInteractionRepository.findByExerciseId(exId);
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertTrue(result.stream().anyMatch(r -> exId.equals(Objects.requireNonNull(r.exercise).id)));
    }

    @Test
    @TestTransaction
    public void testFindByExerciseId_null() {
        final List<AiInteractionEntity> result = this.aiInteractionRepository.findByExerciseId(null);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    @TestTransaction
    public void testPersist_returnsPersistedEntity() {
        final AiInteractionEntity interaction = new AiInteractionEntity();
        interaction.sessionId = "sess-persist";
        interaction.eventType = "expand";
        interaction.feedbackType = "HINT";
        final AiInteractionEntity result = this.aiInteractionRepository.persist(interaction);
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.id);
        Assertions.assertNotNull(result.publicId);
    }

    @Test
    @TestTransaction
    public void testPersist_null() {
        Assertions.assertNull(this.aiInteractionRepository.persist(null));
    }
}
