package de.vptr.aimathtutor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.service.AnalyticsService;
import jakarta.transaction.Transactional;

@DisplayName("Service methods that take session credentials declare their transaction contract")
class ServiceCredentialsTransactionalTest {

    private static final String SERVICE_PACKAGE = AnalyticsService.class.getPackageName();

    @Test
    void testCredentialMethodsAreTransactional() throws IOException, URISyntaxException {
        final var classes = serviceClasses();
        assertFalse(classes.isEmpty(), "No compiled service classes found under " + SERVICE_PACKAGE);

        final var missing = classes.stream().filter(type -> !type.isAnnotationPresent(Transactional.class))
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods())).filter(method -> !method.isSynthetic())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> Arrays.asList(method.getParameterTypes()).contains(SessionCredentials.class))
                .filter(method -> !method.isAnnotationPresent(Transactional.class)).map(Method::toGenericString)
                .sorted().toList();

        // MANDATORY for a guard that joins its caller's transaction, NEVER for one that must commit before it acts,
        // SUPPORTS for one that must hold no connection across slow work and opens its own short transactions inside
        assertTrue(missing.isEmpty(),
                "Public service methods taking SessionCredentials need @Transactional: " + missing);
    }

    private static List<Class<?>> serviceClasses() throws IOException, URISyntaxException {
        final var loader = Thread.currentThread().getContextClassLoader();
        final var classes = new ArrayList<Class<?>>();
        final var roots = loader.getResources(SERVICE_PACKAGE.replace('.', '/'));
        while (roots.hasMoreElements()) {
            final var root = Path.of(roots.nextElement().toURI());
            try (Stream<Path> files = Files.walk(root)) {
                for (final var file : files.filter(path -> path.toString().endsWith(".class")).toList()) {
                    final var relative = root.relativize(file).toString();
                    final var name =
                            SERVICE_PACKAGE + "." + relative.substring(0, relative.length() - ".class".length())
                                    .replace(file.getFileSystem().getSeparator(), ".");
                    try {
                        classes.add(Class.forName(name, false, loader));
                    } catch (final ClassNotFoundException e) {
                        throw new IllegalStateException("Cannot load " + name, e);
                    }
                }
            } catch (final UncheckedIOException e) {
                throw e.getCause();
            }
        }
        return classes;
    }
}
