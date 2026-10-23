package de.vptr.aimathtutor.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
     * Maps a client address to the key its login throttle buckets use. An IPv6 client gets its /64 prefix, because one
     * subscriber typically controls a whole /64 and could otherwise use a fresh address per attempt; an IPv4 address
     * (an IPv4-mapped IPv6 address included) stays whole.
     * <p>
     * Limitation: only IPv4-mapped addresses are keyed as IPv4. Other IPv6 forms that embed an IPv4 address are keyed
     * by their /64 like any IPv6 address, so behind NAT64/SIIT ({@code 64:ff9b::/96}), with IPv4-compatible
     * {@code ::a.b.c.d} addresses or via Teredo, all such clients share one bucket.
     *
     * @param clientIp
     *            the resolved client address, possibly not an IP literal
     * @return the normalised IPv4 address, or the IPv6 /64 prefix as {@code <prefix>/64}; the text unchanged if it is
     *         not an IP literal; {@code null} for {@code null}
     */
    @Nullable
    public static String loginClientKey(@Nullable final String clientIp) {
        final var address = parseLiteral(clientIp);
        if (address == null) {
            return clientIp;
        }
        if (address instanceof Inet6Address) {
            final var prefix = new byte[16];
            System.arraycopy(address.getAddress(), 0, prefix, 0, 8);
            try {
                // From a byte array, so the result carries no scope id
                return InetAddress.getByAddress(prefix).getHostAddress() + "/64";
            } catch (final UnknownHostException e) {
                throw new IllegalStateException("A 16-byte array is an address", e);
            }
        }
        return address.getHostAddress();
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
