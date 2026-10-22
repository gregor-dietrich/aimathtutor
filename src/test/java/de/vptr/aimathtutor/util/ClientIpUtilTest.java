package de.vptr.aimathtutor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.InetAddress;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.annotation.Nullable;

// The subject of this test is IP address literals
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
@DisplayName("ClientIpUtil")
class ClientIpUtilTest {

    private static final String PEER = "192.0.2.10";
    private static final String PROXY2 = "192.0.2.11";
    private static final String CLIENT = "198.51.100.7";
    private static final String FORGED = "203.0.113.9";

    private static InetAddress ip(final String literal) {
        return Objects.requireNonNull(ClientIpUtil.parseLiteral(literal));
    }

    private static Set<InetAddress> trusted(final String... addresses) {
        return Stream.of(addresses).map(ClientIpUtilTest::ip).collect(Collectors.toSet());
    }

    @Nullable
    private static String resolve(final String peer, final List<String> header, final String... trusted) {
        return ClientIpUtil.resolveClientIp(peer, header, trusted(trusted));
    }

    @Test
    @DisplayName("no header returns the peer")
    void noHeaderReturnsPeer() {
        assertEquals(PEER, resolve(PEER, List.of(), PEER));
    }

    @Test
    @DisplayName("an untrusted peer ignores the header")
    void untrustedPeerIgnoresHeader() {
        assertEquals(PEER, resolve(PEER, List.of(FORGED), PROXY2));
    }

    @Test
    @DisplayName("the rightmost entry wins over a forged leading entry")
    void rightmostEntryWins() {
        assertEquals(CLIENT, resolve(PEER, List.of(FORGED + ", " + CLIENT), PEER));
    }

    @Test
    @DisplayName("trusted proxies on the right are skipped")
    void trustedProxiesSkipped() {
        assertEquals(CLIENT, resolve(PEER, List.of(FORGED + ", " + CLIENT + ", " + PROXY2), PEER, PROXY2));
    }

    @Test
    @DisplayName("all entries trusted returns the peer")
    void allTrustedReturnsPeer() {
        assertEquals(PEER, resolve(PEER, List.of(PROXY2 + ", " + PEER), PEER, PROXY2));
    }

    @ParameterizedTest
    @ValueSource(strings = { "unknown", "198.51.100.7:80", "", "evil.example", "localhost", "[198.51.100.7]x" })
    @DisplayName("an unparseable rightmost entry returns the peer, never the entry to its left")
    void unparseableRightmostReturnsPeer(final String rightmost) {
        assertEquals(PEER, resolve(PEER, List.of(FORGED + ", " + rightmost), PEER));
    }

    @Test
    @DisplayName("a trailing comma returns the peer")
    void trailingCommaReturnsPeer() {
        assertEquals(PEER, resolve(PEER, List.of(FORGED + ","), PEER));
    }

    @Test
    @DisplayName("an unparseable entry behind a trusted one returns the peer")
    void unparseableBehindTrustedReturnsPeer() {
        assertEquals(PEER, resolve(PEER, List.of(FORGED + ", unknown, " + PROXY2), PEER, PROXY2));
    }

    @Test
    @DisplayName("IPv6 addresses are normalised")
    void ipv6Normalised() {
        assertEquals("2001:db8:0:0:0:0:0:1", resolve(PEER, List.of("2001:DB8::1"), PEER));
    }

    @Test
    @DisplayName("a trusted compressed IPv6 address matches the expanded peer")
    void trustedIpv6MatchesExpandedPeer() {
        assertEquals(CLIENT, resolve("0:0:0:0:0:0:0:1", List.of(CLIENT), "::1"));
    }

    @Test
    @DisplayName("an IPv4-mapped IPv6 address equals the IPv4 address")
    void ipv4MappedEqualsIpv4() {
        assertEquals("192.0.2.1", resolve(PEER, List.of("::ffff:192.0.2.1"), PEER));
    }

    @Test
    @DisplayName("an IPv4-mapped trusted proxy matches an IPv4 peer")
    void ipv4MappedTrustedMatches() {
        assertEquals(CLIENT, resolve("192.0.2.1", List.of(CLIENT), "::ffff:192.0.2.1"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "10.0.0", "10.1", "12345", "2130706433" })
    @DisplayName("parseLiteral rejects IPv4 short forms")
    void parseLiteralRejectsShortForms(final String shortForm) {
        assertNull(ClientIpUtil.parseLiteral(shortForm));
    }

    @Test
    @DisplayName("parseLiteral still accepts dotted quads and IPv4-mapped forms")
    void parseLiteralAcceptsFullForms() {
        assertEquals(PEER, ip(PEER).getHostAddress());
        assertEquals("192.0.2.1", ip("::ffff:192.0.2.1").getHostAddress());
        assertEquals("192.0.2.1", ip("[::ffff:192.0.2.1]").getHostAddress());
        assertEquals("0:0:0:0:0:0:0:1", ip("[::1]").getHostAddress());
    }

    @Test
    @DisplayName("a short-form rightmost entry returns the peer")
    void shortFormRightmostReturnsPeer() {
        assertEquals(PEER, resolve(PEER, List.of(FORGED + ", 198.51.100"), PEER));
    }

    @Test
    @DisplayName("several header lines count as one list")
    void headerLinesJoined() {
        assertEquals(CLIENT, resolve(PEER, List.of(FORGED, CLIENT), PEER));
    }

    @Test
    @DisplayName("an unparseable peer is returned raw")
    void unparseablePeerReturnedRaw() {
        assertEquals("not-an-ip", resolve("not-an-ip", List.of(FORGED), PEER));
        assertNull(ClientIpUtil.resolveClientIp(null, List.of(FORGED), trusted(PEER)));
    }

    @Test
    @DisplayName("parseLiteral rejects null and blank, trims otherwise")
    void parseLiteral() {
        assertNull(ClientIpUtil.parseLiteral(null));
        assertNull(ClientIpUtil.parseLiteral("  "));
        assertNull(ClientIpUtil.parseLiteral("localhost"));
        assertNotNull(ClientIpUtil.parseLiteral(" " + CLIENT + " "));
    }

    @Test
    @DisplayName("throttleKey keys IPv6 addresses by their /64 prefix")
    void throttleKeyUsesIpv6Prefix() {
        final var key = ClientIpUtil.throttleKey("2001:db8:1:2::1");
        assertEquals("2001:db8:1:2:0:0:0:0/64", key);
        assertEquals(key, ClientIpUtil.throttleKey("2001:db8:1:2:ffff:eeee:dddd:cccc"));
        assertEquals(key, ClientIpUtil.throttleKey("2001:0db8:0001:0002:0000:0000:0000:0001"));
        assertEquals(key, ClientIpUtil.throttleKey("2001:DB8:1:2::1"));
        assertNotEquals(key, ClientIpUtil.throttleKey("2001:db8:1:3::1"));
    }

    @Test
    @DisplayName("throttleKey keeps IPv4 whole, IPv4-mapped IPv6 included")
    void throttleKeyKeepsIpv4() {
        assertEquals(CLIENT, ClientIpUtil.throttleKey(CLIENT));
        assertEquals(CLIENT, ClientIpUtil.throttleKey("::ffff:" + CLIENT));
        assertNotEquals(CLIENT, ClientIpUtil.throttleKey(FORGED));
    }

    @Test
    @DisplayName("throttleKey returns null for null and other text unchanged")
    void throttleKeyPassesThroughNonAddresses() {
        assertNull(ClientIpUtil.throttleKey(null));
        assertEquals("not-an-ip", ClientIpUtil.throttleKey("not-an-ip"));
    }
}
