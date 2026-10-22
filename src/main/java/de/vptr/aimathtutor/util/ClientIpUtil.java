package de.vptr.aimathtutor.util;

import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.annotation.Nullable;

/**
 * Resolves the client address of a request that may have passed through reverse proxies.
 * <p>
 * Addresses are parsed only with {@link InetAddress#ofLiteral(String)}, which never performs a DNS lookup.
 */
public final class ClientIpUtil {

    // ofLiteral also accepts BSD short forms such as "10.1" or "2130706433"; only four decimal parts are an address
    private static final Pattern DOTTED_QUAD = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");

    private ClientIpUtil() {
        // Utility class
    }

    /**
     * Parses an IP address literal.
     *
     * @param value
     *            the text to parse, surrounding whitespace is ignored
     * @return the address, or {@code null} if the text is null, blank or not an IP address literal
     */
    @Nullable
    public static InetAddress parseLiteral(@Nullable final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        final var text = value.trim();
        if (text.indexOf(':') < 0 && !DOTTED_QUAD.matcher(text).matches()) {
            return null;
        }
        try {
            return InetAddress.ofLiteral(text);
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Resolves the client address. {@code X-Forwarded-For} is honoured only when the direct peer is a trusted proxy:
     * the entries of all header lines are then walked from the right, skipping trusted proxies, and the first other
     * entry is the client. An entry that is not an IP address stops the walk and the direct peer counts as the client,
     * as it does when every entry is trusted.
     *
     * @param remoteAddr
     *            the address of the direct TCP peer
     * @param forwardedFor
     *            the values of every {@code X-Forwarded-For} header line, possibly empty
     * @param trusted
     *            the addresses of the trusted proxies
     * @return the client address; {@code remoteAddr} unchanged if it is not an IP address
     */
    @Nullable
    public static String resolveClientIp(@Nullable final String remoteAddr, final List<String> forwardedFor,
            final Set<InetAddress> trusted) {
        final var peer = parseLiteral(remoteAddr);
        if (peer == null) {
            return remoteAddr;
        }
        if (forwardedFor.isEmpty() || !trusted.contains(peer)) {
            return peer.getHostAddress();
        }
        final var entries = String.join(",", forwardedFor).split(",", -1);
        for (var i = entries.length - 1; i >= 0; i--) {
            final var entry = parseLiteral(entries[i]);
            if (entry == null) {
                return peer.getHostAddress();
            }
            if (!trusted.contains(entry)) {
                return entry.getHostAddress();
            }
        }
        return peer.getHostAddress();
    }
}
