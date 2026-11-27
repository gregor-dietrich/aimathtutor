package de.vptr.aimathtutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Dockerfiles pin the same Temurin version, each by digest")
class TemurinBasePinTest {

    private static final Path ALPINE = Path.of("src/main/docker/Dockerfile.alpine");

    private static final Path UBUNTU = Path.of("src/main/docker/Dockerfile.ubuntu");

    private static final Pattern FROM_LINE = Pattern.compile("(?i)^\\h*FROM(?:\\h.*)?$");

    private static final Pattern TEMURIN_PIN =
            Pattern.compile("(?i:FROM)\\h+eclipse-temurin:(\\d+(?:\\.\\d+)*_\\d+)-jre-[a-z]+@sha256:[a-f0-9]{64}");

    @Test
    void testTemurinBasePinsMatch() throws IOException {
        assertEquals(version(ALPINE), version(UBUNTU), "Both Dockerfiles must use the same Temurin version");
    }

    private static String version(final Path dockerfile) throws IOException {
        final var instructions = Files.readAllLines(dockerfile).stream()
                .filter(line -> FROM_LINE.matcher(line).matches()).map(String::strip).toList();
        assertEquals(1, instructions.size(), dockerfile + " must have exactly one FROM instruction");
        final var pin = TEMURIN_PIN.matcher(instructions.get(0));
        assertTrue(pin.matches(),
                dockerfile
                        + " FROM must be exactly eclipse-temurin:<version>_<build>-jre-<distro>@sha256:<64 hex digits>,"
                        + " <distro> in letters only: Dependabot reads a release number there as a build number");
        return pin.group(1);
    }
}
