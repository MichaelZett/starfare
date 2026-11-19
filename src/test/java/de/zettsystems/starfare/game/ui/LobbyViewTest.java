package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.html.Div;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The entry route shows the own start's lobby and stays empty inside a host, which forwards it. */
class LobbyViewTest {

    @Test
    void staysEmptyWithoutTheOwnStartsParts() {
        assertThat(new LobbyView(null).getComponentCount()).isZero();
    }

    @Test
    void showsTheLobbyOfTheOwnStart() {
        Div lobby = new Div();
        OwnFrameParts parts = new OwnFrameParts() {
            @Override public Div lobby() { return lobby; }
            @Override public Div languageSwitcher() { return new Div(); }
            @Override public Div accountNavigation() { return new Div(); }
            @Override public Div legalFooter() { return new Div(); }
        };
        assertThat(new LobbyView(parts).getChildren()).containsExactly(lobby);
    }
}
