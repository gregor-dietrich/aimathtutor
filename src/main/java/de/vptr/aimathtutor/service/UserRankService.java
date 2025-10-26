package de.vptr.aimathtutor.service;

import java.util.List;
import java.util.Optional;

import com.vaadin.flow.server.VaadinSession;

import de.vptr.aimathtutor.dto.UserRankDto;
import de.vptr.aimathtutor.dto.UserRankViewDto;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class UserRankService {

    private static final String USERNAME_KEY = "authenticated.username";

    @Transactional
    public UserRankViewDto getCurrentUserRank() {
        final var session = VaadinSession.getCurrent();
        if (session == null) {
            return null; // Return null instead of throwing when no session
        }

        final var username = (String) session.getAttribute(USERNAME_KEY);
        if (username == null) {
            return null; // Return null instead of throwing when not authenticated
        }

        final var user = UserEntity.<UserEntity>find("username = ?1", username).firstResult();
        if (user == null || user.rank == null) {
            return null; // Return null instead of throwing when user or rank not found
        }

        return new UserRankViewDto(user.rank);
    }

    @Transactional
    public List<UserRankViewDto> getAllRanks() {
        return UserRankEntity.find("ORDER BY id DESC").list().stream()
                .map(entity -> new UserRankViewDto((UserRankEntity) entity))
                .toList();
    }

    @Transactional
    public Optional<UserRankViewDto> findById(final Long id) {
        return UserRankEntity.findByIdOptional(id)
                .map(entity -> new UserRankViewDto((UserRankEntity) entity));
    }

    public Optional<UserRankViewDto> findByName(final String name) {
        return UserRankEntity.find("name", name).firstResultOptional()
                .map(entity -> new UserRankViewDto((UserRankEntity) entity));
    }

    @Transactional
    public List<UserRankViewDto> searchRanks(final String query) {
        if (query == null || query.trim().isEmpty()) {
            return this.getAllRanks();
        }
        final var searchTerm = "%" + query.trim().toLowerCase() + "%";
        final List<UserRankEntity> ranks = UserRankEntity.find("LOWER(name) LIKE ?1 ORDER BY id DESC", searchTerm)
                .list();
        return ranks.stream()
                .map(UserRankViewDto::new)
                .toList();
    }

    @Transactional
    public UserRankViewDto createRank(final UserRankDto rankDto) {
        final UserRankEntity rank = new UserRankEntity();

        // Set properties from DTO
        rank.name = rankDto.name;

        // Set permissions from DTO with defaults
        rank.adminView = rankDto.adminView != null ? rankDto.adminView : false;
        rank.exerciseAdd = rankDto.exerciseAdd != null ? rankDto.exerciseAdd : false;
        rank.exerciseDelete = rankDto.exerciseDelete != null ? rankDto.exerciseDelete : false;
        rank.exerciseEdit = rankDto.exerciseEdit != null ? rankDto.exerciseEdit : false;
        rank.lessonAdd = rankDto.lessonAdd != null ? rankDto.lessonAdd : false;
        rank.lessonDelete = rankDto.lessonDelete != null ? rankDto.lessonDelete : false;
        rank.lessonEdit = rankDto.lessonEdit != null ? rankDto.lessonEdit : false;
        rank.commentAdd = rankDto.commentAdd != null ? rankDto.commentAdd : false;
        rank.commentDelete = rankDto.commentDelete != null ? rankDto.commentDelete : false;
        rank.commentEdit = rankDto.commentEdit != null ? rankDto.commentEdit : false;
        rank.userAdd = rankDto.userAdd != null ? rankDto.userAdd : false;
        rank.userDelete = rankDto.userDelete != null ? rankDto.userDelete : false;
        rank.userEdit = rankDto.userEdit != null ? rankDto.userEdit : false;
        rank.userGroupAdd = rankDto.userGroupAdd != null ? rankDto.userGroupAdd : false;
        rank.userGroupDelete = rankDto.userGroupDelete != null ? rankDto.userGroupDelete : false;
        rank.userGroupEdit = rankDto.userGroupEdit != null ? rankDto.userGroupEdit : false;
        rank.userRankAdd = rankDto.userRankAdd != null ? rankDto.userRankAdd : false;
        rank.userRankDelete = rankDto.userRankDelete != null ? rankDto.userRankDelete : false;
        rank.userRankEdit = rankDto.userRankEdit != null ? rankDto.userRankEdit : false;

        rank.persist();
        return new UserRankViewDto(rank);
    }

    @Transactional
    public UserRankViewDto updateRank(final Long id, final UserRankDto rankDto) {
        final UserRankEntity existingRank = UserRankEntity.findById(id);
        if (existingRank == null) {
            throw new WebApplicationException("User rank not found", Response.Status.NOT_FOUND);
        }

        // Complete replacement (PUT semantics)
        existingRank.name = rankDto.name;
        existingRank.adminView = rankDto.adminView != null ? rankDto.adminView : false;
        existingRank.exerciseAdd = rankDto.exerciseAdd != null ? rankDto.exerciseAdd : false;
        existingRank.exerciseDelete = rankDto.exerciseDelete != null ? rankDto.exerciseDelete : false;
        existingRank.exerciseEdit = rankDto.exerciseEdit != null ? rankDto.exerciseEdit : false;
        existingRank.lessonAdd = rankDto.lessonAdd != null ? rankDto.lessonAdd : false;
        existingRank.lessonDelete = rankDto.lessonDelete != null ? rankDto.lessonDelete : false;
        existingRank.lessonEdit = rankDto.lessonEdit != null ? rankDto.lessonEdit : false;
        existingRank.commentAdd = rankDto.commentAdd != null ? rankDto.commentAdd : false;
        existingRank.commentDelete = rankDto.commentDelete != null ? rankDto.commentDelete : false;
        existingRank.commentEdit = rankDto.commentEdit != null ? rankDto.commentEdit : false;
        existingRank.userAdd = rankDto.userAdd != null ? rankDto.userAdd : false;
        existingRank.userDelete = rankDto.userDelete != null ? rankDto.userDelete : false;
        existingRank.userEdit = rankDto.userEdit != null ? rankDto.userEdit : false;
        existingRank.userGroupAdd = rankDto.userGroupAdd != null ? rankDto.userGroupAdd : false;
        existingRank.userGroupDelete = rankDto.userGroupDelete != null ? rankDto.userGroupDelete : false;
        existingRank.userGroupEdit = rankDto.userGroupEdit != null ? rankDto.userGroupEdit : false;
        existingRank.userRankAdd = rankDto.userRankAdd != null ? rankDto.userRankAdd : false;
        existingRank.userRankDelete = rankDto.userRankDelete != null ? rankDto.userRankDelete : false;
        existingRank.userRankEdit = rankDto.userRankEdit != null ? rankDto.userRankEdit : false;

        existingRank.persist();
        return new UserRankViewDto(existingRank);
    }

    @Transactional
    public UserRankViewDto patchRank(final Long id, final UserRankDto rankDto) {
        final UserRankEntity existingRank = UserRankEntity.findById(id);
        if (existingRank == null) {
            throw new WebApplicationException("User rank not found", Response.Status.NOT_FOUND);
        }

        // Partial update (PATCH semantics) - only update provided fields
        if (rankDto.name != null)
            existingRank.name = rankDto.name;
        if (rankDto.adminView != null)
            existingRank.adminView = rankDto.adminView;
        if (rankDto.exerciseAdd != null)
            existingRank.exerciseAdd = rankDto.exerciseAdd;
        if (rankDto.exerciseDelete != null)
            existingRank.exerciseDelete = rankDto.exerciseDelete;
        if (rankDto.exerciseEdit != null)
            existingRank.exerciseEdit = rankDto.exerciseEdit;
        if (rankDto.lessonAdd != null)
            existingRank.lessonAdd = rankDto.lessonAdd;
        if (rankDto.lessonDelete != null)
            existingRank.lessonDelete = rankDto.lessonDelete;
        if (rankDto.lessonEdit != null)
            existingRank.lessonEdit = rankDto.lessonEdit;
        if (rankDto.commentAdd != null)
            existingRank.commentAdd = rankDto.commentAdd;
        if (rankDto.commentDelete != null)
            existingRank.commentDelete = rankDto.commentDelete;
        if (rankDto.commentEdit != null)
            existingRank.commentEdit = rankDto.commentEdit;
        if (rankDto.userAdd != null)
            existingRank.userAdd = rankDto.userAdd;
        if (rankDto.userDelete != null)
            existingRank.userDelete = rankDto.userDelete;
        if (rankDto.userEdit != null)
            existingRank.userEdit = rankDto.userEdit;
        if (rankDto.userGroupAdd != null)
            existingRank.userGroupAdd = rankDto.userGroupAdd;
        if (rankDto.userGroupDelete != null)
            existingRank.userGroupDelete = rankDto.userGroupDelete;
        if (rankDto.userGroupEdit != null)
            existingRank.userGroupEdit = rankDto.userGroupEdit;
        if (rankDto.userRankAdd != null)
            existingRank.userRankAdd = rankDto.userRankAdd;
        if (rankDto.userRankDelete != null)
            existingRank.userRankDelete = rankDto.userRankDelete;
        if (rankDto.userRankEdit != null)
            existingRank.userRankEdit = rankDto.userRankEdit;

        existingRank.persist();
        return new UserRankViewDto(existingRank);
    }

    @Transactional
    public boolean deleteRank(final Long id) {
        final UserRankEntity rank = UserRankEntity.findById(id);
        if (rank == null) {
            return false;
        }

        // Check if rank has associated users
        final List<UserEntity> usersWithRank = UserEntity.find("rank.id = ?1", id).list();
        if (!usersWithRank.isEmpty()) {
            throw new WebApplicationException(
                    "Cannot delete rank because " + usersWithRank.size() + " user(s) are assigned to this rank. " +
                            "Please reassign these users to a different rank before deleting.",
                    Response.Status.CONFLICT);
        }

        return UserRankEntity.deleteById(id);
    }
}
