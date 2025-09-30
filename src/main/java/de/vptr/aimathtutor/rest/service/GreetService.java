package de.vptr.aimathtutor.rest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class GreetService {

    private static final Logger LOG = LoggerFactory.getLogger(GreetService.class);

    @Inject
    AuthService authService;

    public String greet(final String name) {
        LOG.debug("GreetService.greet called with name: {}", name);

        try {
            if (!this.authService.isAuthenticated()) {
                return "Error: Not authenticated";
            }

            var greeting = "Hello, ";
            if (name == null || name.isEmpty()) {
                greeting += "anonymous user";
            } else {
                greeting += name;
            }

            return greeting + "!";
        } catch (final Exception e) {
            LOG.error("Error in greeting service", e);
            return "Error: Could not generate greeting";
        }
    }
}
