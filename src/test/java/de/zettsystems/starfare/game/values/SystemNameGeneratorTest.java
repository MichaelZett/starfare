package de.zettsystems.starfare.game.values;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SystemNameGeneratorTest {

    @Test
    void sampleReturnsRequestedCount() {
        List<String> names = SystemNameGenerator.sample(8, new Random(1));

        assertThat(names).hasSize(8);
    }

    @Test
    void sampleIsDeterministicForTheSameSeed() {
        List<String> a = SystemNameGenerator.sample(10, new Random(42));
        List<String> b = SystemNameGenerator.sample(10, new Random(42));

        assertThat(b).containsExactlyElementsOf(a);
    }

    @Test
    void sampleProducesDistinctNamesWhenWithinPool() {
        List<String> names = SystemNameGenerator.sample(5, new Random(0));

        assertThat(Set.copyOf(names)).as("names within pool size should be unique").hasSize(5);
    }

    @Test
    void sampleAppendsRomanSuffixesWhenCountExceedsPool() {
        // Ask for more than the pool can deliver — overflow names get "II"/"III"...
        int huge = 5_000;
        List<String> names = SystemNameGenerator.sample(huge, new Random(0));

        assertThat(names).hasSize(huge);
        long withSuffix = names.stream()
                .filter(n -> n.endsWith(" II") || n.endsWith(" III") || n.endsWith(" IV"))
                .count();
        assertThat(withSuffix > 0).as("overflow batch must reuse pool with roman suffixes").isTrue();
    }
}
