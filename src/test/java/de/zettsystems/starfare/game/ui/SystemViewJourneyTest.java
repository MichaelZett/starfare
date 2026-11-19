package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The home system of a fresh standard game shows its stars, worlds and shipyard, describes a selected
 * world, keeps the same bodies after a reload and follows a language change.
 */
class SystemViewJourneyTest extends AbstractMapJourneyTest {

    @Test
    void theHomeSystemIsStableAcrossReloadAndLanguage() {
        email = loginAsNewPlayer("System Tester");
        account = String.valueOf(accounts.findByEmail(email).orElseThrow().id());
        id = games.newGame(GameSetup.defaults(), account, "System view");
        player = games.joinGame(id, account, "System Tester", "System Empire").orElseThrow();
        assertThat(games.startGame(id)).isTrue();
        openMap();
        assertThat(headerStats()).containsExactly("1", "4", "8");

        String home = homeSystem();
        select(home);
        assertThat(find(Component.class).withClassName("system-star").all()).hasSizeBetween(1, 2);
        assertThat(worlds()).hasSizeBetween(2, 7);
        assertThat(text("system-shipyard")).contains("Werft");
        List<String> bodies = bodyLabels();
        test(worlds().getFirst()).click();
        assertThat(find(Component.class).withClassName("system-body-description").single()
                .getElement().getTextRecursively()).contains("Umlaufbahn");
        assertThat(find(com.vaadin.flow.component.dialog.Dialog.class).all()).noneMatch(
                com.vaadin.flow.component.dialog.Dialog::isOpened);

        reloadMap();
        select(home);
        assertThat(bodyLabels()).isEqualTo(bodies);
        assertThat(headerStats()).containsExactly("1", "4", "8");

        switchLanguage(Locale.ENGLISH);
        reloadMap();
        assertThat(viewText()).contains("Submit turn");
        select(home);
        assertThat(text("system-shipyard")).contains("Shipyard");
        assertThat(text("system-schematic-caption")).contains("Star reduced");

        switchLanguage(Locale.GERMAN);
        reloadMap();
        select(home);
        assertThat(text("system-shipyard")).contains("Werft");
        assertThat(text("system-schematic-caption")).contains("Stern verkleinert");
    }

    private String homeSystem() {
        int seat = player;
        return registry.readState(id, state -> state.systems().stream()
                .filter(system -> Objects.equals(system.ownerId(), seat)).map(StarSystem::name).findFirst().orElseThrow());
    }

    private List<Button> worlds() {
        return find(Button.class).withClassName("system-world").all();
    }

    private List<String> bodyLabels() {
        return find(Button.class).withClassName("system-body-target").all().stream()
                .map(target -> target.getAriaLabel().orElse("")).toList();
    }

    private List<String> headerStats() {
        return find(Span.class).withClassName("app-header-stat-value").all().stream()
                .map(stat -> stat.getElement().getTextRecursively().strip()).toList();
    }
}
