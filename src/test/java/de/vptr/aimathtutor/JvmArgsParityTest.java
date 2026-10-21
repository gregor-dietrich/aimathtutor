package de.vptr.aimathtutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Runtime JVM arguments stay aligned across the images and the dev JVMs")
class JvmArgsParityTest {

    private static final Path POM = Path.of("pom.xml");

    private static final Path JVM_CONFIG = Path.of(".mvn/jvm.config");

    private static final Path ALPINE = Path.of("src/main/docker/Dockerfile.alpine");

    private static final Path UBUNTU = Path.of("src/main/docker/Dockerfile.ubuntu");

    private static final Pattern POM_JVM_ARGS = Pattern.compile("(?s)<jvmArgs>(.*?)</jvmArgs>");

    private static final Pattern ENTRYPOINT = Pattern.compile("(?ms)^ENTRYPOINT\\h*\\[(.*?)\\]");

    private static final Pattern QUOTED = Pattern.compile("\"([^\"]*)\"");

    private static final String COMPILER_MODULE = "jdk.compiler/";

    private static final List<String> LAUNCHER_TAIL = List.of("-jar", "/deployments/quarkus-run.jar");

    @Test
    void testImagesShareOneEntrypoint() throws IOException {
        final var alpine = entrypoint(ALPINE);
        final var ubuntu = entrypoint(UBUNTU);
        assertEquals(alpine, ubuntu, "Both Dockerfiles must carry the same ENTRYPOINT");
    }

    @Test
    void testDevJvmsCarryEveryRuntimeArgument() throws IOException {
        final var runtime = runtimeArguments();
        assertFalse(runtime.isEmpty(), "The images must carry runtime JVM arguments");
        for (final var dev : List.of(POM, JVM_CONFIG)) {
            final var missing = new TreeSet<>(runtime);
            missing.removeAll(devArguments(dev));
            assertEquals(Set.of(), missing, dev + " lacks runtime arguments of the images");
        }
    }

    @Test
    void testCompilerAccessOnlyInDevJvms() throws IOException {
        final var fromPom = compilerArguments(devArguments(POM));
        assertFalse(fromPom.isEmpty(), "pom.xml must open jdk.compiler for Error Prone");
        assertEquals(fromPom, compilerArguments(devArguments(JVM_CONFIG)),
                "pom.xml and .mvn/jvm.config must carry the same jdk.compiler arguments");
        for (final var image : List.of(ALPINE, UBUNTU)) {
            assertEquals(Set.of(), compilerArguments(Set.copyOf(entrypoint(image))),
                    image + " must carry no jdk.compiler arguments");
        }
    }

    private static Set<String> runtimeArguments() throws IOException {
        final var arguments = new TreeSet<>(entrypoint(ALPINE));
        arguments.removeIf(argument -> argument.startsWith("-D"));
        return arguments;
    }

    private static Set<String> compilerArguments(final Set<String> arguments) {
        final var compiler = new TreeSet<String>();
        arguments.stream().filter(argument -> argument.contains(COMPILER_MODULE)).forEach(compiler::add);
        return compiler;
    }

    private static Set<String> devArguments(final Path file) throws IOException {
        final var content = Files.readString(file);
        if (file.equals(POM)) {
            final var jvmArgs = POM_JVM_ARGS.matcher(content);
            assertTrue(jvmArgs.find(), file + " must declare jvmArgs");
            final var arguments = jvmArgs.group(1);
            assertFalse(jvmArgs.find(), file + " must declare jvmArgs exactly once");
            return new TreeSet<>(fold(split(arguments)));
        }
        return new TreeSet<>(fold(split(content)));
    }

    private static List<String> split(final String text) {
        return text.isBlank() ? List.of() : List.of(text.strip().split("\\s+"));
    }

    private static List<String> entrypoint(final Path dockerfile) throws IOException {
        final var entrypoint = ENTRYPOINT.matcher(Files.readString(dockerfile));
        assertTrue(entrypoint.find(), dockerfile + " must declare an ENTRYPOINT");
        final var body = entrypoint.group(1);
        assertFalse(entrypoint.find(), dockerfile + " must declare exactly one ENTRYPOINT");
        final var tokens = new ArrayList<String>();
        final var quoted = QUOTED.matcher(body);
        while (quoted.find()) {
            tokens.add(quoted.group(1));
        }
        assertEquals("java", tokens.get(0), dockerfile + " ENTRYPOINT must start with java");
        assertEquals(LAUNCHER_TAIL, tokens.subList(tokens.size() - 2, tokens.size()),
                dockerfile + " ENTRYPOINT must end with the launcher jar");
        return fold(tokens.subList(1, tokens.size() - 2));
    }

    /** Joins each {@code --add-opens}/{@code --add-exports} with its value, as one argument. */
    private static List<String> fold(final List<String> tokens) {
        final var folded = new ArrayList<String>();
        var prefix = "";
        for (final var token : tokens) {
            if ("--add-opens".equals(token) || "--add-exports".equals(token)) {
                prefix = token + " ";
            } else {
                folded.add(prefix + token);
                prefix = "";
            }
        }
        return folded;
    }
}
