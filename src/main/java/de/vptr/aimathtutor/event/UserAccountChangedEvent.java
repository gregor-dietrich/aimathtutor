package de.vptr.aimathtutor.event;

import jakarta.annotation.Nullable;

/**
 * CDI event fired by {@code UserService} on every user write that can end the account's sessions: a password change, a
 * rename, a banned or activated flag change, or a deletion. It is observed after the transaction commits by
 * {@code AuthService}, which evicts the cached authentication state.
 *
 * @param publicId
 *            the public ID of the changed account
 * @param credentialStamp
 *            the account's new credential stamp (see {@code AuthService.credentialStamp}), or null when the account was
 *            deleted
 */
public record UserAccountChangedEvent(String publicId, @Nullable String credentialStamp) {
}
