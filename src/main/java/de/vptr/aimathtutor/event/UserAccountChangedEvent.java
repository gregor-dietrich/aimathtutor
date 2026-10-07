package de.vptr.aimathtutor.event;

import jakarta.annotation.Nullable;

/**
 * CDI event fired by {@code UserService} on every user update, password change and deletion (a rename no longer ends
 * sessions, since they are bound to the public ID). It is observed after the transaction commits by
 * {@code AuthService}, which evicts the cached authentication state.
 *
 * @param publicId
 *            the public ID of the changed account
 * @param credentialStamp
 *            the account's stamp after the change (see {@code AuthService.credentialStamp}), null when deleted
 */
public record UserAccountChangedEvent(String publicId, @Nullable String credentialStamp) {
}
