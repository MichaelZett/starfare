package de.zettsystems.starfare.game.values;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class GameNameGenerator {

    private static final List<String> PREFIX = List.of(
            "Nebel", "Sturm", "Schlacht", "Aufstand", "Fall", "Zorn", "Exodus",
            "Chronik", "Krieg", "Aufbruch", "Eklipse", "Ära", "Legion", "Echo"
    );

    private static final List<String> OF = List.of("von", "um", "über", "bei", "jenseits von");

    private static final List<String> PLACE = List.of(
            "Vega", "Altair", "Sirius", "Rigel", "Procyon", "Arcturus", "Capella",
            "Betelgeuse", "Aldebaran", "Deneb", "Antares", "Orion", "Andromeda",
            "Cygnus", "Draco", "Perseus", "Kepler-22", "Tau Ceti", "Proxima"
    );

    private GameNameGenerator() {
    }

    public static String random() {
        var r = ThreadLocalRandom.current();
        return PREFIX.get(r.nextInt(PREFIX.size()))
                + " " + OF.get(r.nextInt(OF.size()))
                + " " + PLACE.get(r.nextInt(PLACE.size()));
    }
}
