package de.zettsystems.starfare.style.ui;

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
}
