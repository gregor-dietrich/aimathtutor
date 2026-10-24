package de.vptr.aimathtutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.Permission;

@DisplayName("The README's administrator recovery SQL grants every permission")
class AdminRecoverySqlTest {

    private static final Pattern RANK_UPDATE = Pattern.compile("(?s)UPDATE user_ranks SET.*?;");

    private static final Pattern GRANTED_COLUMN = Pattern.compile("(\\w+) = TRUE");

    @Test
    void recoverySqlGrantsExactlyTheEnumsColumns() throws IOException {
        final var matcher = RANK_UPDATE.matcher(Files.readString(Path.of("README.md")));
        assertTrue(matcher.find(), "README.md has no UPDATE user_ranks statement");

        final Set<String> granted =
                GRANTED_COLUMN.matcher(matcher.group()).results().map(r -> r.group(1)).collect(Collectors.toSet());

        assertEquals(Arrays.stream(Permission.values()).map(Permission::column).collect(Collectors.toSet()), granted);
    }
}
