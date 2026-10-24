package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import de.zettsystems.starfare.economy.values.SystemQuality;
import de.zettsystems.starfare.game.values.SystemComposition;
import jakarta.annotation.security.PermitAll;
import java.util.List;

/** Uses production rendering but exists only on the browser test classpath. */
@Route("acceptance/system-gallery")
@PermitAll
public class SystemGalleryAcceptanceView extends Div {
    public SystemGalleryAcceptanceView() {
        setId("system-gallery");
        add(new LanguageSwitcher());
        for (var star : SystemComposition.StarType.values()) {
            addCard(de.zettsystems.starfare.i18n.I18n.t("system.star." + star.name().toLowerCase(java.util.Locale.ROOT)), star.name(), new SystemComposition(List.of(star), List.of(
                    body(1, SystemComposition.BodyType.ROCKY), body(2, SystemComposition.BodyType.GAS_GIANT))));
        }
        for (var type : SystemComposition.BodyType.values()) {
            addCard(de.zettsystems.starfare.i18n.I18n.t("system.body." + type.name().toLowerCase(java.util.Locale.ROOT)), type.name(), new SystemComposition(List.of(SystemComposition.StarType.YELLOW), List.of(body(1, type))));
        }
        addColony("Nahe Bahn · 0,8 AE", "near", 0.8, false, SystemQuality.Atmosphere.TEMPERATE);
        addColony("Ferne Bahn · 2,4 AE", "far", 2.4, false, SystemQuality.Atmosphere.TEMPERATE);
        addColony("Besiedelter Mond", "moon", 1, true, SystemQuality.Atmosphere.TEMPERATE);
        addColony("Keine Atmosphäre", "airless", 1, false, SystemQuality.Atmosphere.NONE);
        addColony("Dichte Atmosphäre", "dense", 1, false, SystemQuality.Atmosphere.DENSE);
        addColony("Dünne Atmosphäre", "thin", 1, false, SystemQuality.Atmosphere.THIN);
        addCard("Doppelstern · sieben Bahnen", "crowded", new SystemComposition(
                List.of(SystemComposition.StarType.WHITE, SystemComposition.StarType.RED),
                java.util.stream.IntStream.rangeClosed(1, 7).mapToObj(index -> body(index,
                        SystemComposition.BodyType.values()[(index - 1) % 6])).toList()));
    }
    private void addColony(String label, String id, double orbit, boolean moon, SystemQuality.Atmosphere atmosphere) {
        var quality = new SystemQuality(SystemComposition.StarType.YELLOW, 1, orbit,
                moon ? SystemComposition.BodyType.GAS_GIANT : SystemComposition.BodyType.ROCKY, moon, atmosphere, 3);
        addCard(label, id, new SystemComposition(List.of(quality.star()), List.of(body(1, quality.body())), quality));
    }
    private void addCard(String label, String id, SystemComposition composition) {
        var panel = new DetailedSystemPanel(composition, false,
                de.zettsystems.starfare.game.values.GameId.of("system-gallery-" + id), 1);
        Div card = new Div(new H2(label), panel);
        card.setId("gallery-" + id);
        card.addClassName("system-gallery-card");
        Span inspected = new Span();
        inspected.addClassName("gallery-colony-inspected");
        panel.onColonyInspect(() -> inspected.setText("Kolonie geöffnet"));
        card.add(inspected);
        add(card);
    }
    private static SystemComposition.Body body(int orbit, SystemComposition.BodyType type) {
        return new SystemComposition.Body(orbit, type, 0, type == SystemComposition.BodyType.GAS_GIANT, 0);
    }
}
