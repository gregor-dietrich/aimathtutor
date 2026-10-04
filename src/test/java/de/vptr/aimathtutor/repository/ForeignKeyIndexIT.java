package de.vptr.aimathtutor.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Guards the invariant that V2 established: every foreign key has an index whose leading columns are the foreign key's
 * columns, so deletes on the referenced table and joins on the key don't scan the referencing table.
 */
@QuarkusTest
class ForeignKeyIndexIT {

    // An index covers a foreign key when each of the key's n columns sits among the index's first n columns.
    // No array slices: Hibernate would read the slice's ":" as a named parameter.
    private static final String UNINDEXED_FOREIGN_KEYS = """
            SELECT c.conrelid::regclass || '.' || c.conname
            FROM pg_constraint c
            WHERE c.contype = 'f'
              AND NOT EXISTS (
                SELECT 1 FROM pg_index i
                WHERE i.indrelid = c.conrelid
                  AND NOT EXISTS (
                    SELECT 1 FROM unnest(c.conkey) AS k(attnum)
                    WHERE coalesce(array_position(string_to_array(i.indkey::text, ' ')::int2[], k.attnum),
                                   cardinality(c.conkey) + 1) > cardinality(c.conkey)))
            ORDER BY 1
            """;

    @Inject
    EntityManager em;

    @Test
    void everyForeignKeyHasAnIndexOnItsLeadingColumns() {
        assertEquals(List.of(), this.findUnindexedForeignKeys());
    }

    @Test
    @TestTransaction
    void reportsAForeignKeyWhoseIndexWasDropped() {
        // Control: PostgreSQL DDL is transactional, so the test transaction's rollback restores the index
        this.em.createNativeQuery("DROP INDEX idx_session_user_start").executeUpdate();
        assertEquals(List.of("student_sessions.student_sessions_user_id_fkey"), this.findUnindexedForeignKeys());
    }

    private List<String> findUnindexedForeignKeys() {
        final List<?> rows = this.em.createNativeQuery(UNINDEXED_FOREIGN_KEYS, String.class).getResultList();
        return rows.stream().map(String.class::cast).toList();
    }
}
