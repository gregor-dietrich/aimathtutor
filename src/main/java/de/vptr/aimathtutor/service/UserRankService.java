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
        return UserRankEntity.listAll().stream()
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

    public List<UserRankViewDto> searchRanks(final String query) {
        if (query == null || query.trim().isEmpty()) {
            return this.getAllRanks();
        }
        final var searchTerm = "%" + query.trim().toLowerCase() + "%";
        final List<UserRankEntity> ranks = UserRankEntity.find("LOWER(name) LIKE ?1", searchTerm).list();
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
        rank.pageAdd = rankDto.pageAdd != null ? rankDto.pageAdd : false;
        rank.pageDelete = rankDto.pageDelete != null ? rankDto.pageDelete : false;
        rank.pageEdit = rankDto.pageEdit != null ? rankDto.pageEdit : false;
        rank.postAdd = rankDto.postAdd != null ? rankDto.postAdd : false;
        rank.postDelete = rankDto.postDelete != null ? rankDto.postDelete : false;
        rank.postEdit = rankDto.postEdit != null ? rankDto.postEdit : false;
        rank.postCategoryAdd = rankDto.postCategoryAdd != null ? rankDto.postCategoryAdd : false;
        rank.postCategoryDelete = rankDto.postCategoryDelete != null ? rankDto.postCategoryDelete : false;
        rank.postCategoryEdit = rankDto.postCategoryEdit != null ? rankDto.postCategoryEdit : false;
        rank.postCommentAdd = rankDto.postCommentAdd != null ? rankDto.postCommentAdd : false;
        rank.postCommentDelete = rankDto.postCommentDelete != null ? rankDto.postCommentDelete : false;
        rank.postCommentEdit = rankDto.postCommentEdit != null ? rankDto.postCommentEdit : false;
        rank.userAdd = rankDto.userAdd != null ? rankDto.userAdd : false;
        rank.userDelete = rankDto.userDelete != null ? rankDto.userDelete : false;
        rank.userEdit = rankDto.userEdit != null ? rankDto.userEdit : false;
        rank.userGroupAdd = rankDto.userGroupAdd != null ? rankDto.userGroupAdd : false;
        rank.userGroupDelete = rankDto.userGroupDelete != null ? rankDto.userGroupDelete : false;
        rank.userGroupEdit = rankDto.userGroupEdit != null ? rankDto.userGroupEdit : false;
        rank.userAccountAdd = rankDto.userAccountAdd != null ? rankDto.userAccountAdd : false;
        rank.userAccountDelete = rankDto.userAccountDelete != null ? rankDto.userAccountDelete : false;
        rank.userAccountEdit = rankDto.userAccountEdit != null ? rankDto.userAccountEdit : false;
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
        existingRank.pageAdd = rankDto.pageAdd != null ? rankDto.pageAdd : false;
        existingRank.pageDelete = rankDto.pageDelete != null ? rankDto.pageDelete : false;
        existingRank.pageEdit = rankDto.pageEdit != null ? rankDto.pageEdit : false;
        existingRank.postAdd = rankDto.postAdd != null ? rankDto.postAdd : false;
        existingRank.postDelete = rankDto.postDelete != null ? rankDto.postDelete : false;
        existingRank.postEdit = rankDto.postEdit != null ? rankDto.postEdit : false;
        existingRank.postCategoryAdd = rankDto.postCategoryAdd != null ? rankDto.postCategoryAdd : false;
        existingRank.postCategoryDelete = rankDto.postCategoryDelete != null ? rankDto.postCategoryDelete : false;
        existingRank.postCategoryEdit = rankDto.postCategoryEdit != null ? rankDto.postCategoryEdit : false;
        existingRank.postCommentAdd = rankDto.postCommentAdd != null ? rankDto.postCommentAdd : false;
        existingRank.postCommentDelete = rankDto.postCommentDelete != null ? rankDto.postCommentDelete : false;
        existingRank.postCommentEdit = rankDto.postCommentEdit != null ? rankDto.postCommentEdit : false;
        existingRank.userAdd = rankDto.userAdd != null ? rankDto.userAdd : false;
        existingRank.userDelete = rankDto.userDelete != null ? rankDto.userDelete : false;
        existingRank.userEdit = rankDto.userEdit != null ? rankDto.userEdit : false;
        existingRank.userGroupAdd = rankDto.userGroupAdd != null ? rankDto.userGroupAdd : false;
        existingRank.userGroupDelete = rankDto.userGroupDelete != null ? rankDto.userGroupDelete : false;
        existingRank.userGroupEdit = rankDto.userGroupEdit != null ? rankDto.userGroupEdit : false;
        existingRank.userAccountAdd = rankDto.userAccountAdd != null ? rankDto.userAccountAdd : false;
        existingRank.userAccountDelete = rankDto.userAccountDelete != null ? rankDto.userAccountDelete : false;
        existingRank.userAccountEdit = rankDto.userAccountEdit != null ? rankDto.userAccountEdit : false;
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
        if (rankDto.pageAdd != null)
            existingRank.pageAdd = rankDto.pageAdd;
        if (rankDto.pageDelete != null)
            existingRank.pageDelete = rankDto.pageDelete;
        if (rankDto.pageEdit != null)
            existingRank.pageEdit = rankDto.pageEdit;
        if (rankDto.postAdd != null)
            existingRank.postAdd = rankDto.postAdd;
        if (rankDto.postDelete != null)
            existingRank.postDelete = rankDto.postDelete;
        if (rankDto.postEdit != null)
            existingRank.postEdit = rankDto.postEdit;
        if (rankDto.postCategoryAdd != null)
            existingRank.postCategoryAdd = rankDto.postCategoryAdd;
        if (rankDto.postCategoryDelete != null)
            existingRank.postCategoryDelete = rankDto.postCategoryDelete;
        if (rankDto.postCategoryEdit != null)
            existingRank.postCategoryEdit = rankDto.postCategoryEdit;
        if (rankDto.postCommentAdd != null)
            existingRank.postCommentAdd = rankDto.postCommentAdd;
        if (rankDto.postCommentDelete != null)
            existingRank.postCommentDelete = rankDto.postCommentDelete;
        if (rankDto.postCommentEdit != null)
            existingRank.postCommentEdit = rankDto.postCommentEdit;
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
        if (rankDto.userAccountAdd != null)
            existingRank.userAccountAdd = rankDto.userAccountAdd;
        if (rankDto.userAccountDelete != null)
            existingRank.userAccountDelete = rankDto.userAccountDelete;
        if (rankDto.userAccountEdit != null)
            existingRank.userAccountEdit = rankDto.userAccountEdit;
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
        return UserRankEntity.deleteById(id);
    }
}
