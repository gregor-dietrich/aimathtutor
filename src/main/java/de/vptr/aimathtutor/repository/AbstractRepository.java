package de.vptr.aimathtutor.repository;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * Abstract base class for all repository implementations.
 * Provides common functionality for database access and named query execution.
 */
@ApplicationScoped
public abstract class AbstractRepository {

    @Inject
    protected EntityManager em;

    /**
     * Executes a named query and retrieves all results of the specified type.
     *
     * @param <T>  the type of entities to retrieve
     * @param name the name of the persistent named query
     * @param type the entity class type
     * @return a list of all results from the named query
     */
    protected <T> List<T> listNamed(final String name, final Class<T> type) {
        final TypedQuery<T> q = this.em.createNamedQuery(name, type);
        return q.getResultList();
    }

    /**
     * Executes a named query with a maximum results limit and retrieves results of
     * the specified type.
     *
     * @param <T>  the type of entities to retrieve
     * @param name the name of the persistent named query
     * @param type the entity class type
     * @param max  the maximum number of results to retrieve (0 or negative returns
     *             empty list)
     * @return a list of up to max results from the named query
     */
    protected <T> List<T> listNamedWithMax(final String name, final Class<T> type, final int max) {
        final TypedQuery<T> q = this.em.createNamedQuery(name, type);
        q.setMaxResults(Math.max(0, max));
        return q.getResultList();
    }

}
