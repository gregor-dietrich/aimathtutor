package de.vptr.aimathtutor.repository;

import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.entity.LessonEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class LessonRepository {

    @Inject
    EntityManager em;

    public LessonEntity findById(final Long id) {
        if (id == null) {
            return null;
        }
        return this.em.find(LessonEntity.class, id);
    }

    public Optional<LessonEntity> findByIdOptional(final Long id) {
        return Optional.ofNullable(this.findById(id));
    }

    public List<LessonEntity> findAllOrdered() {
        final TypedQuery<LessonEntity> q = this.em.createQuery("FROM LessonEntity ORDER BY id DESC",
                LessonEntity.class);
        return q.getResultList();
    }

    public List<LessonEntity> findRootLessons() {
        final TypedQuery<LessonEntity> q = this.em.createQuery(
                "FROM LessonEntity WHERE parent IS NULL ORDER BY id DESC",
                LessonEntity.class);
        return q.getResultList();
    }

    public List<LessonEntity> findByParentId(final Long parentId) {
        final TypedQuery<LessonEntity> q = this.em.createQuery(
                "FROM LessonEntity WHERE parent.id = :p ORDER BY id DESC",
                LessonEntity.class);
        q.setParameter("p", parentId);
        return q.getResultList();
    }

    public List<LessonEntity> search(final String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return this.findAllOrdered();
        }
        final var pattern = "%" + searchTerm.trim().toLowerCase() + "%";
        final TypedQuery<LessonEntity> q = this.em.createQuery("FROM LessonEntity WHERE LOWER(name) LIKE :s",
                LessonEntity.class);
        q.setParameter("s", pattern);
        return q.getResultList();
    }

    @Transactional
    public boolean deleteById(final Long id) {
        final LessonEntity e = this.findById(id);
        if (e == null) {
            return false;
        }
        this.em.remove(e);
        return true;
    }

    @Transactional
    public LessonEntity persist(final LessonEntity lesson) {
        if (lesson == null) {
            return null;
        }
        this.em.persist(lesson);
        return lesson;
    }

}
