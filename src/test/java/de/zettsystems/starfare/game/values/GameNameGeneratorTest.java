package de.zettsystems.starfare.game.values;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class GameNameGeneratorTest {

    @RepeatedTest(20)
    void randomNameIsNonBlankAndHasAtLeastThreeTokens() {
        String name = GameNameGenerator.random();

        assertThat(name)
                .isNotBlank();
        // Format is "<prefix> <connector> <place>", but the connector ("jenseits von")
        // and some places ("Tau Ceti") add extra spaces, so we only assert the lower bound.
        String[] parts = name.split(" ");
        assertThat(parts).as("expected >=3 tokens, got '" + name + "'").hasSizeGreaterThanOrEqualTo(3);
    }

    @RepeatedTest(5)
    void randomNamesAreNotAlwaysIdentical() {
        // Sanity check that the generator actually picks different combinations.
        // With ~14*5*19 = 1330 combinations, two draws of 25 should almost certainly differ.
        long distinct = IntStream.range(0, 25)
                .mapToObj(_ -> GameNameGenerator.random())
                .distinct()
                .count();
        assertThat(distinct).as("generator should produce variation across draws").isGreaterThan(1);
    }

    @Test
    void randomNameUsesGermanConnectorVocabulary() {
        // Must contain one of the documented connectors in the middle slot.
        String name = GameNameGenerator.random();

        boolean hasConnector = List.of(" von ", " um ", " über ", " bei ", " jenseits von ")
                .stream().anyMatch(name::contains);
        assertThat(hasConnector).as("name '" + name + "' has no known connector").isTrue();
    }
}
