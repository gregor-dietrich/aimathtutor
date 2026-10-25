package de.vptr.aimathtutor.service.ai;

import java.net.InetAddress;
import java.net.UnknownHostException;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Resolves host names to addresses. It is the only path by which {@link AiConfigService} reaches DNS, so that tests
 * replace it and no test resolves a real host name.
 */
@ApplicationScoped
public class HostResolver {

    /**
     * Resolves every address of a host.
     *
     * @param host
     *            the host name or address literal
     * @return all addresses the host resolves to
     * @throws UnknownHostException
     *             when the host cannot be resolved
     */
    public InetAddress[] resolveAll(final String host) throws UnknownHostException {
        return InetAddress.getAllByName(host);
    }
}
