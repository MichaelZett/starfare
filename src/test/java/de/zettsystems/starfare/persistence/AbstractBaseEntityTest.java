package de.zettsystems.starfare.persistence;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractBaseEntityTest {

    @Test
    void anEntityEqualsItselfEvenBeforeItHasAnId() {
        Ship transientShip = new Ship(null);
        Ship persistedShip = new Ship(4L);

        assertThat(sameAs(transientShip, transientShip)).isTrue();
        assertThat(sameAs(persistedShip, persistedShip)).isTrue();
    }

    @Test
    void anEntityNeverEqualsNull() {
        assertThat(sameAs(new Ship(4L), null)).isFalse();
        assertThat(sameAs(new Ship(null), null)).isFalse();
    }

    @Test
    void entitiesOfTheSameTypeAreEqualExactlyWhenTheirIdsAre() {
        assertThat(new Ship(4L)).isEqualTo(new Ship(4L)).hasSameHashCodeAs(new Ship(4L));
        assertThat(new Ship(4L)).isNotEqualTo(new Ship(5L));
    }

    @Test
    void entitiesWithoutAnIdAreOnlyEqualToThemselves() {
        Ship transientShip = new Ship(null);

        assertThat(transientShip).isNotEqualTo(new Ship(null)).isNotEqualTo(new Ship(4L));
        assertThat(new Ship(4L)).isNotEqualTo(transientShip);
        assertThat(transientShip.hashCode()).isEqualTo(System.identityHashCode(transientShip));
    }

    @Test
    void entitiesOfDifferentTypesAreNeverEqualEvenWithTheSameId() {
        assertThat(new Ship(4L)).isNotEqualTo(new Planet(4L));
        assertThat(new Planet(4L)).isNotEqualTo(new Ship(4L));
        assertThat(new Ship(4L)).isNotEqualTo("4");
    }

    @Test
    void theHashCodeOfAPersistedEntityIsTheHashCodeOfItsId() {
        assertThat(new Ship(4L)).hasSameHashCodeAs(4L);
    }

    /** Calls equals directly: AssertJ short-cuts identity and null before equals is reached. */
    private static boolean sameAs(Object entity, @Nullable Object other) {
        return entity.equals(other);
    }

    private static final class Ship extends AbstractBaseEntity<Long> {
        private final @Nullable Long id;

        Ship(@Nullable Long id) {
            this.id = id;
        }

        @Override
        public @Nullable Long getId() {
            return id;
        }
    }

    private static final class Planet extends AbstractBaseEntity<Long> {
        private final @Nullable Long id;

        Planet(@Nullable Long id) {
            this.id = id;
        }

        @Override
        public @Nullable Long getId() {
            return id;
        }
    }
}
