package de.vptr.aimathtutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PostgreSQL image pins stay aligned across Compose and Dev Services")
class PostgresImagePinTest {

    private static final Pattern SERVICES = Pattern.compile("(?ms)^services:\\h*\\R(.*?)(?=^[^\\s#]|\\z)");

    private static final Pattern DATABASE = Pattern.compile("(?ms)^  db:\\h*\\R(.*?)(?=^  [^\\s#]|\\z)");

    private static final Pattern IMAGE = Pattern.compile("(?m)^    image:\\h*(\\S+)\\h*$");

    private static final Pattern POSTGRES_PIN = Pattern.compile("(postgres:[^\\s@]+)@(sha256:[a-f0-9]{64})");

    private static final Pattern DEV_SERVICES_PIN =
            Pattern.compile("(?m)^# Dev Services PostgreSQL tag: (postgres:[^\\s@]+)\\R"
                    + "%dev,test\\.quarkus\\.datasource\\.devservices\\.image-name=postgres@(sha256:[a-f0-9]{64})$");

    @Test
    void testPostgresImagePinsMatch() throws IOException {
        final var developmentImage = databaseImage(Path.of("docker-compose.yml"));
        final var productionImage = databaseImage(Path.of("deploy/compose.yml"));
        assertEquals(developmentImage, productionImage, "Compose db images must have the same exact tag and digest");

        final var composePin = POSTGRES_PIN.matcher(developmentImage);
        assertTrue(composePin.matches(), "Compose db image must have a PostgreSQL tag and SHA-256 digest");
        final var properties = Files.readString(Path.of("src/main/resources/application.properties"));
        final var devServicesPin = DEV_SERVICES_PIN.matcher(properties);
        assertTrue(devServicesPin.find(), "Dev Services needs a digest-only pin immediately after its tag comment");
        assertEquals(1, properties.split("devservices\\.image-name=", -1).length - 1,
                "A second Dev Services image-name property would override the checked pin");
        assertEquals(composePin.group(1), devServicesPin.group(1), "Dev Services tag comment must match Compose");
        assertEquals(composePin.group(2), devServicesPin.group(2), "Dev Services digest must match Compose");
    }

    private static String databaseImage(final Path composeFile) throws IOException {
        final var services = SERVICES.matcher(Files.readString(composeFile));
        assertTrue(services.find(), composeFile + " must declare services");
        final var database = DATABASE.matcher(services.group(1));
        assertTrue(database.find(), composeFile + " must declare a db service");
        final var image = IMAGE.matcher(database.group(1));
        assertTrue(image.find(), composeFile + " db service must declare an image");
        return image.group(1);
    }
}
