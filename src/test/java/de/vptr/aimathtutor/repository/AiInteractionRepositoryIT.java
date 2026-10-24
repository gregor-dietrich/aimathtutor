package de.vptr.aimathtutor.repository;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.AiInteractionEntity;
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
