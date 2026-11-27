package de.zettsystems.starfare.game.ui;

import de.zettsystems.gamesplatformapi.PlatformSettings;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** How the choices of the last new game are written into one setting and read back. */
class NewGameMemoryTest {

    @Test
    void choicesSurviveTheRoundTripAndBrokenPairsAreSkipped() {
        Map<String, String> choices = new LinkedHashMap<>();
        choices.put("players", "4");
        choices.put("events", "SUPERNOVA,PIRATES");
        assertThat(NewGameMemory.parse(NewGameMemory.format(choices))).isEqualTo(choices);
        assertThat(NewGameMemory.parse("players=4;;=7;broken;tempo=")).containsExactly(
                Map.entry("players", "4"), Map.entry("tempo", ""));
    }

    @Test
    void withoutAccountOrPlatformNothingIsKept() {
        RecordingSettings settings = new RecordingSettings();
        NewGameMemory.of(settings, null).remember(Map.of("players", "4"));
        NewGameMemory.none().remember(Map.of("players", "4"));

        assertThat(settings.values).isEmpty();
        assertThat(NewGameMemory.of(null, "7").recall()).isEmpty();
    }

    @Test
    void aValueTooLongForThePlatformIsNotWritten() {
        RecordingSettings settings = new RecordingSettings();
        NewGameMemory memory = NewGameMemory.of(settings, "7");

        memory.remember(Map.of("events", "X".repeat(PlatformSettings.MAX_VALUE_LENGTH)));
        assertThat(settings.values).isEmpty();

        memory.remember(Map.of("players", "4"));
        assertThat(memory.recall()).containsExactly(Map.entry("players", "4"));
    }

    private static final class RecordingSettings implements PlatformSettings {
        private final Map<String, String> values = new HashMap<>();

        @Override
        public java.util.Optional<String> value(String gameKey, String key) {
            return java.util.Optional.ofNullable(values.get(gameKey + "/" + key));
        }

        @Override
        public Map<String, String> values(String gameKey, java.util.Collection<String> keys) {
            Map<String, String> found = new HashMap<>();
            keys.forEach(key -> value(gameKey, key).ifPresent(value -> found.put(key, value)));
            return found;
        }

        @Override
        public void set(String gameKey, String key, String value) {
            values.put(gameKey + "/" + key, value);
        }

        @Override
        public void reset(String gameKey, String key) {
            values.remove(gameKey + "/" + key);
        }
    }
}
