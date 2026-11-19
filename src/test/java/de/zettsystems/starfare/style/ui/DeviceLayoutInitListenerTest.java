package de.zettsystems.starfare.style.ui;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import de.zettsystems.starfare.style.values.DeviceLayout;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceLayoutInitListenerTest {

    @Test
    void readsTheOverrideFromTheInitialLocation() {
        assertThat(DeviceLayoutInitListener.fromLocation("map/42?layout=mobile")).contains(DeviceLayout.MOBILE);
        assertThat(DeviceLayoutInitListener.fromLocation("?foo=1&layout=desktop&bar")).contains(DeviceLayout.DESKTOP);
    }

    @Test
    void readsTheOverrideFromTheInitialQuery() {
        assertThat(DeviceLayoutInitListener.fromQuery("layout=mobile")).contains(DeviceLayout.MOBILE);
        assertThat(DeviceLayoutInitListener.fromQuery("a=b&layout=desktop")).contains(DeviceLayout.DESKTOP);
        assertThat(DeviceLayoutInitListener.fromQuery("")).isEmpty();
        assertThat(DeviceLayoutInitListener.fromQuery(null)).isEmpty();
    }

    @Test
    void ignoresLocationsWithoutOverride() {
        assertThat(DeviceLayoutInitListener.fromLocation("map/42")).isEmpty();
        assertThat(DeviceLayoutInitListener.fromLocation("map/42?layouts=mobile&")).isEmpty();
        assertThat(DeviceLayoutInitListener.fromLocation(null)).isEmpty();
    }

    @Test
    void onlyStarfareRoutesCountAsStarfare() {
        assertThat(DeviceLayoutInitListener.ownsRoute(de.zettsystems.starfare.game.ui.LobbyView.class)).isTrue();
        assertThat(DeviceLayoutInitListener.ownsRoute(com.vaadin.flow.router.RouteNotFoundError.class)).isFalse();
        assertThat(DeviceLayoutInitListener.ownsRoute(null)).isFalse();
    }

    @Test
    void loadsTheStylesheetsOnceOnEnteringAndRemovesThemOnLeaving() {
        UI ui = new UI();

        DeviceLayoutInitListener.follow(ui, true);
        DeviceLayoutInitListener.follow(ui, true);
        var loaded = ComponentUtil.getData(ui, DeviceLayoutInitListener.Loaded.class);
        assertThat(loaded).isNotNull();
        assertThat(loaded.sheets()).hasSize(DeviceLayoutInitListener.STYLESHEETS.size());
        assertThat(loaded.mobile()).isFalse();

        DeviceLayoutInitListener.follow(ui, false);
        assertThat(ComponentUtil.getData(ui, DeviceLayoutInitListener.Loaded.class)).isNull();
        DeviceLayoutInitListener.follow(ui, true);
        assertThat(ComponentUtil.getData(ui, DeviceLayoutInitListener.Loaded.class)).isNotNull();
    }
}
