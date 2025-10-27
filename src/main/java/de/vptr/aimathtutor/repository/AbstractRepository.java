package de.vptr.aimathtutor.repository;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * TODO: Class documentation.
 */
@ApplicationScoped
public abstract class AbstractRepository {

    @Inject
    protected EntityManager em;

    /**
     * TODO: Document listNamed().
     */
    protected <T> List<T> listNamed(final String name, final Class<T> type) {
        final TypedQuery<T> q = this.em.createNamedQuery(name, type);
        return q.getResultList();
    }

    /**
     * TODO: Document listNamedWithMax().
     */
    protected <T> List<T> listNamedWithMax(final String name, final Class<T> type, final int max) {
        final TypedQuery<T> q = this.em.createNamedQuery(name, type);
        q.setMaxResults(Math.max(0, max));
        return q.getResultList();
    }

}
