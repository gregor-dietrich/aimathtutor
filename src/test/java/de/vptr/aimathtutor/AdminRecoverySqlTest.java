package de.vptr.aimathtutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.Permission;

@DisplayName("The README's administrator recovery SQL restores an active administrator")
class AdminRecoverySqlTest {

    private static final Pattern RANK_UPDATE =
            Pattern.compile("(?s)UPDATE user_ranks SET(.*?)WHERE public_id = '(\\w+)';");

    private static final Pattern USER_UPDATE = Pattern.compile("(?s)UPDATE users SET password = (.*?)rank_id = "
            + "\\(SELECT id FROM user_ranks WHERE public_id = '(\\w+)'\\)");

    private static final Pattern GRANTED_COLUMN = Pattern.compile("(\\w+) = TRUE");

    @Test
    void recoverySqlGrantsExactlyTheEnumsColumns() throws IOException {
        final Set<String> granted = GRANTED_COLUMN.matcher(find(RANK_UPDATE).group(1)).results().map(r -> r.group(1))
                .collect(Collectors.toSet());

        assertEquals(Arrays.stream(Permission.values()).map(Permission::column).collect(Collectors.toSet()), granted);
    }

    @Test
    void recoverySqlActivatesTheAccountOnTheRestoredRank() throws IOException {
        final var userUpdate = find(USER_UPDATE);

        assertEquals(find(RANK_UPDATE).group(2), userUpdate.group(2), "the users UPDATE assigns another rank");
        assertTrue(userUpdate.group(1).contains("banned = FALSE"), "the users UPDATE does not unban");
        assertTrue(userUpdate.group(1).contains("activated = TRUE"), "the users UPDATE does not activate");
    }

    private static MatchResult find(final Pattern statement) throws IOException {
        final var matcher = statement.matcher(Files.readString(Path.of("README.md")));
        assertTrue(matcher.find(), "README.md has no statement matching " + statement);
        return matcher.toMatchResult();
    }
}
