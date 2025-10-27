package de.vptr.aimathtutor.repository;

import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.entity.UserRankEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserRankRepository extends AbstractRepository {

    public List<UserRankEntity> findAll() {
        return listNamed("UserRank.findAll", UserRankEntity.class);
    }

    public Optional<UserRankEntity> findByIdOptional(final Long id) {
        return Optional.ofNullable(this.findById(id));
    }

    public UserRankEntity findById(final Long id) {
        if (id == null) {
            return null;
        }
        return this.em.find(UserRankEntity.class, id);
    }

    public Optional<UserRankEntity> findByName(final String name) {
        final var q = this.em.createNamedQuery("UserRank.findByName", UserRankEntity.class);
        q.setParameter("n", name);
        q.setMaxResults(1);
        return q.getResultStream().findFirst();
    }

    public List<UserRankEntity> search(final String searchTerm) {
        final var q = this.em.createNamedQuery("UserRank.searchByName", UserRankEntity.class);
        q.setParameter("s", searchTerm);
        return q.getResultList();
    }

    @Transactional
    public void persist(final UserRankEntity rank) {
        if (rank == null) {
            return;
        }
        this.em.persist(rank);
    }

    @Transactional
    public boolean deleteById(final Long id) {
        final UserRankEntity e = this.findById(id);
        if (e == null) {
            return false;
        }
        this.em.remove(e);
        return true;
    }
}
