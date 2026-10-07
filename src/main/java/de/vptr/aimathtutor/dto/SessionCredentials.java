package de.vptr.aimathtutor.dto;

/**
 * The credentials of a signed-in session, captured on the UI thread and passed to off-thread work so it can verify the
 * session inside its own transaction.
 *
 * @param userPublicId
 *            the public ID of the session's user
 * @param credentialStamp
 *            the credential stamp the session was issued, see {@code AuthService.credentialStamp}
 * @param throttleKey
 *            the random per-session token that keys the session's password-change throttle
 */
public record SessionCredentials(String userPublicId, String credentialStamp, String throttleKey) {
}
