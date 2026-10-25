package de.vptr.aimathtutor.service.ai;

import java.net.InetAddress;
import java.net.UnknownHostException;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Replaces {@link HostResolver} in every {@code @QuarkusTest}, so that no test resolves a real host name. An address
 * literal resolves to itself and any other name to {@link #PUBLIC_ADDRESS}. A test that needs specific answers or
 * interaction checks declares {@code @InjectMock HostResolver}, and stubs the default with
 * {@code thenCallRealMethod()}.
 */
@Mock
@ApplicationScoped
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public class TestHostResolver extends HostResolver {

    /** Google Public DNS: globally routable unicast, in no IANA special-purpose registry entry. */
    public static final String PUBLIC_ADDRESS = "8.8.8.8";

    @Override
    public InetAddress[] resolveAll(final String host) throws UnknownHostException {
        return new InetAddress[] { InetAddress.ofLiteral(isLiteral(host) ? host : PUBLIC_ADDRESS) };
    }

    private static boolean isLiteral(final String host) {
        try {
            InetAddress.ofLiteral(host);
            return true;
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }
}
