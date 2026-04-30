package de.zettsystems.starfare.i18n;

import de.zettsystems.starfare.game.ui.UiTexts;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class TranslationKeysCoverageTest {

    private static final String DE_BUNDLE = "/vaadin-i18n/translations.properties";
    private static final String EN_BUNDLE = "/vaadin-i18n/translations_en.properties";

    /**
     * Keys not declared in {@link UiTexts} but emitted as opaque strings from the application layer
     * (see PlayerViewBuilder.PlannedOrder.type). Add when introducing new application-emitted keys.
     */
    private static final Set<String> APPLICATION_LAYER_KEYS = Set.of(
            "map.orderType.send",
            "map.orderType.wait",
            "map.orderType.disband"
    );

    @Test
    void everyUiTextsKeyHasGermanTranslation() throws Exception {
        Properties de = loadBundle(DE_BUNDLE);
        Set<String> missing = collectUiTextsKeys().stream()
                .filter(k -> !de.containsKey(k))
                .collect(Collectors.toCollection(TreeSet::new));
        assertThat(missing).as("UiTexts keys missing in DE bundle").isEmpty();
    }

    @Test
    void everyUiTextsKeyHasEnglishTranslation() throws Exception {
        Properties en = loadBundle(EN_BUNDLE);
        Set<String> missing = collectUiTextsKeys().stream()
                .filter(k -> !en.containsKey(k))
                .collect(Collectors.toCollection(TreeSet::new));
        assertThat(missing).as("UiTexts keys missing in EN bundle").isEmpty();
    }

    @Test
    void deAndEnBundlesHaveSameKeys() throws Exception {
        Properties de = loadBundle(DE_BUNDLE);
        Properties en = loadBundle(EN_BUNDLE);
        Set<String> deOnly = new TreeSet<>(de.stringPropertyNames());
        deOnly.removeAll(en.stringPropertyNames());
        Set<String> enOnly = new TreeSet<>(en.stringPropertyNames());
        enOnly.removeAll(de.stringPropertyNames());
        assertThat(deOnly).as("Keys present in DE but missing in EN").isEmpty();
        assertThat(enOnly).as("Keys present in EN but missing in DE").isEmpty();
    }

    @Test
    void bundleHasNoOrphanKeys() throws Exception {
        Set<String> known = new TreeSet<>(collectUiTextsKeys());
        known.addAll(APPLICATION_LAYER_KEYS);
        Properties de = loadBundle(DE_BUNDLE);
        Set<String> orphans = de.stringPropertyNames().stream()
                .filter(k -> !known.contains(k))
                .collect(Collectors.toCollection(TreeSet::new));
        assertThat(orphans)
                .as("Bundle keys not declared in UiTexts or APPLICATION_LAYER_KEYS")
                .isEmpty();
    }

    private static Set<String> collectUiTextsKeys() {
        Set<String> keys = new TreeSet<>();
        for (Field f : UiTexts.class.getDeclaredFields()) {
            int mods = f.getModifiers();
            if (Modifier.isPublic(mods) && Modifier.isStatic(mods) && Modifier.isFinal(mods)
                    && f.getType().equals(String.class)) {
                try {
                    keys.add((String) f.get(null));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Cannot read UiTexts field " + f.getName(), e);
                }
            }
        }
        return keys;
    }

    private static Properties loadBundle(String resource) throws IOException {
        InputStream in = TranslationKeysCoverageTest.class.getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalStateException("Bundle not found on classpath: " + resource);
        }
        Properties p = new Properties();
        try (in; InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            p.load(reader);
        }
        return p;
    }
}
