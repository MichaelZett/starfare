package de.zettsystems.starfare.game.values;

import de.zettsystems.gamesplatformapi.Tempo;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class GameTempoTest {

    @Test
    void theGameDescriptionNamesEveryTempoWithItsDeadlines() throws Exception {
        Properties description = new Properties();
        try (InputStream in = GameTempoTest.class.getResourceAsStream("/games/starfare/game.properties")) {
            description.load(in);
        }
        String expected = Arrays.stream(GameTempo.values())
                .map(tempo -> tempo.name() + ":" + text(tempo.roundLimit()) + "/" + text(tempo.stragglerLimit()))
                .collect(Collectors.joining(", "));

        assertThat(description.getProperty("tempos")).isEqualTo(expected);
    }

    @Test
    void everyTempoIsOneOfThePlatforms() {
        assertThat(GameTempo.values()).allSatisfy(tempo -> assertThat(Tempo.valueOf(tempo.name())).isNotNull());
    }

    @Test
    void everyTempoOfThePlatformIsOffered() {
        assertThat(Tempo.values()).allSatisfy(tempo -> assertThat(GameTempo.valueOf(tempo.name())).isNotNull());
        assertThat(GameTempo.EVERY_OTHER_DAY.roundLimit()).isEqualTo(Duration.ofHours(48));
        assertThat(GameTempo.EVERY_OTHER_DAY.stragglerLimit()).isEqualTo(Duration.ofHours(12));
    }

    @Test
    void aTempoSetsBothDeadlinesWhateverIsGiven() {
        RoundRules rules = new RoundRules(Duration.ofDays(5), Duration.ofSeconds(30), null, GameTempo.BRISK);

        assertThat(rules.roundLimit()).isEqualTo(Duration.ofHours(1));
        assertThat(rules.stragglerLimit()).isEqualTo(Duration.ofMinutes(15));
        assertThat(new RoundRules(Duration.ofMinutes(10), null, null).tempo()).isNull();
    }

    private static String text(Duration duration) {
        return duration.toMinutes() % 60 == 0 ? duration.toHours() + "h" : duration.toMinutes() + "m";
    }
}
