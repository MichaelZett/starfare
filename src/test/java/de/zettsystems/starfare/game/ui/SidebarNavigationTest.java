package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.server.VaadinSession;
import de.zettsystems.starfare.game.application.BattleAcknowledgementService;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SidebarNavigationTest {
    // Vaadin keeps only a weak reference to the current session.
    private final VaadinSession session = mock(VaadinSession.class);

    @BeforeEach
    void setSession() { VaadinSession.setCurrent(session); }

    @AfterEach
    void clearSession() {
        VaadinSession.setCurrent(null);
        java.lang.ref.Reference.reachabilityFence(session);
    }

    @Test
    void fourTabsSwitchTheSectionsAndTheReportTabHidesWhenUnavailable() {
        var panel = new FleetAndOrdersPanel(new BattleAcknowledgements(mock(BattleAcknowledgementService.class)),
                _ -> { }, _ -> { }, _ -> { }, () -> { }, _ -> { }, _ -> { }, _ -> { }, _ -> { },
                _ -> { }, _ -> { }, () -> { });
        assertThat(descendants(panel).filter(ComboBox.class::isInstance)).as("no compact section select").isEmpty();
        Tabs tabs = descendants(panel).filter(Tabs.class::isInstance).map(Tabs.class::cast).findFirst().orElseThrow();
        assertThat(tabs.getChildren().map(tab -> tab.getElement().getText()).toList())
                .containsExactly("Details", "Flotten", "Verlegungen", "Bericht");
        panel.showFleets();
        assertThat(tabs.getSelectedIndex()).isEqualTo(1);
        panel.showReport();
        panel.setReportAvailable(false);
        assertThat(tabs.getSelectedIndex()).isZero();
        assertThat(tabs.getChildren().toList().getLast().isVisible()).isFalse();
        panel.setReportAvailable(true);
        assertThat(tabs.getChildren().toList().getLast().isVisible()).isTrue();
        tabs.setSelectedIndex(2);
        assertThat(tabs.getSelectedIndex()).isEqualTo(2);
    }

    private static Stream<Component> descendants(Component component) {
        return component.getChildren().flatMap(child -> Stream.concat(Stream.of(child), descendants(child)));
    }
}
