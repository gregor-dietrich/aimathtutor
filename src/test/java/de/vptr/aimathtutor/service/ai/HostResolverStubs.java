package de.vptr.aimathtutor.service.ai;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Stubs for the mocked {@link HostResolver}, so that no test resolves a real host name.
 */
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
final class HostResolverStubs {

    /** A public documentation address; {@code ofLiteral} does no lookup. */
    private static final String PUBLIC_ADDRESS = "203.0.113.10";

    private HostResolverStubs() {
    }

    /** Makes every host resolve to one public address. Mockito would return null for the array otherwise. */
    static void resolveEverythingPublic(final HostResolver hostResolver) throws UnknownHostException {
        when(hostResolver.resolveAll(anyString()))
                .thenReturn(new InetAddress[] { InetAddress.ofLiteral(PUBLIC_ADDRESS) });
    }
}
