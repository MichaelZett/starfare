package de.zettsystems.starfare.game.values;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class SystemNameGenerator {

    private static final String RESOURCE = "system-names.txt";
    private static final List<String> NAMES = loadNames();

    private SystemNameGenerator() {
    }

    public static List<String> sample(int count, Random random) {
        List<String> pool = new ArrayList<>(NAMES);
        Collections.shuffle(pool, random);
        if (count <= pool.size()) {
            return new ArrayList<>(pool.subList(0, count));
        }
        List<String> out = new ArrayList<>(pool);
        int suffix = 2;
        int cursor = 0;
        while (out.size() < count) {
            out.add(pool.get(cursor) + " " + roman(suffix));
            cursor++;
            if (cursor >= pool.size()) {
                cursor = 0;
                suffix++;
            }
        }
        return out;
    }

    private static List<String> loadNames() {
        List<String> names = new ArrayList<>();
        try (InputStream in = SystemNameGenerator.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return List.of("Sol");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                        names.add(trimmed);
                    }
                }
            }
        } catch (IOException _) {
            return List.of("Sol");
        }
        return names.isEmpty() ? List.of("Sol") : List.copyOf(names);
    }

    private static String roman(int n) {
        return switch (n) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(n);
        };
    }
}
