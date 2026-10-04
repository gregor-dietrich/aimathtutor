package de.vptr.aimathtutor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * quarkus.flyway.locations is fixed at build time, so a {@code @QuarkusTest}, which builds under the test profile,
 * never sees the production value. This resolves application.properties per profile the way the build does, so a
 * misplaced profile prefix that ships {@code db/demo}'s demo accounts to production fails here.
 */
class FlywayLocationsTest {

    @ParameterizedTest
    @CsvSource(delimiter = ';',
            value = { "prod;db/migration", "dev;db/migration,db/demo", "test;db/migration,db/demo" })
    void demoDataLoadsOnlyInDevAndTest(final String profile, final String expected) throws IOException {
        final var properties = Objects.requireNonNull(this.getClass().getResource("/application.properties"));
        final var config = new SmallRyeConfigBuilder().withProfile(profile)
                .withSources(new PropertiesConfigSource(properties)).build();
        assertEquals(List.of(expected.split(",")),
                config.getOptionalValues("quarkus.flyway.locations", String.class).orElse(List.of("db/migration")));
    }
}
