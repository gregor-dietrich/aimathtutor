package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HostResolver Tests")
class HostResolverTest {

    @Test
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    @DisplayName("resolveAll returns the address of an address literal")
    void testResolveAllLiteral() throws UnknownHostException {
        assertArrayEquals(new InetAddress[] { InetAddress.ofLiteral("203.0.113.10") },
                new HostResolver().resolveAll("203.0.113.10"));
    }
}
