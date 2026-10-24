package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.server.VaadinSession;
import de.zettsystems.starfare.game.application.BattleAcknowledgementService;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SidebarNavigationTest {
    @AfterEach
    void clearSession() { VaadinSession.setCurrent(null); }

    @Test
    void compactAndWideNavigationStayInSyncWhenTheReportBecomesUnavailable() {
        VaadinSession.setCurrent(mock(VaadinSession.class));
        var panel = new FleetAndOrdersPanel(new BattleAcknowledgements(mock(BattleAcknowledgementService.class)),
                _ -> { }, _ -> { }, _ -> { }, () -> { }, _ -> { }, _ -> { }, _ -> { }, _ -> { },
                (_, _) -> { }, _ -> { }, _ -> { }, () -> { });
        ComboBox<?> compact = descendants(panel).filter(ComboBox.class::isInstance).map(ComboBox.class::cast).findFirst().orElseThrow();
        Tabs tabs = descendants(panel).filter(Tabs.class::isInstance).map(Tabs.class::cast).findFirst().orElseThrow();
        assertThat(compact.getListDataView().getItemCount()).isEqualTo(7);
        panel.showOrders();
        assertThat(compact.getValue().toString()).isEqualTo("ORDERS");
        assertThat(tabs.getSelectedIndex()).isEqualTo(3);
        panel.showReport();
        panel.setReportAvailable(false);
        assertThat(compact.getListDataView().getItemCount()).isEqualTo(6);
        assertThat(compact.getValue().toString()).isEqualTo("CONTACTS");
        assertThat(tabs.getSelectedIndex()).isZero();
        panel.setReportAvailable(true);
        assertThat(compact.getListDataView().getItemCount()).isEqualTo(7);
        tabs.setSelectedIndex(4);
        assertThat(compact.getValue().toString()).isEqualTo("RELOCATIONS");
    }

    private static Stream<Component> descendants(Component component) {
        return component.getChildren().flatMap(child -> Stream.concat(Stream.of(child), descendants(child)));
    }
}
