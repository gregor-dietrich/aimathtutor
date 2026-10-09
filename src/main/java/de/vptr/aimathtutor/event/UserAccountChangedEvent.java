package de.vptr.aimathtutor.event;

/**
 * CDI event fired by {@code UserService} on every user update, password change and deletion (a rename no longer ends
 * sessions, since they are bound to the public ID). It is observed after the transaction commits by
 * {@code AuthService}, which evicts the cached authentication state.
 *
 * @param publicId
 *            the public ID of the changed account
 */
public record UserAccountChangedEvent(String publicId) {
}
