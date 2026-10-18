package de.zettsystems.starfare.style.values;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceLayoutTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/126.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Android 14; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0"})
    void phonesGetTheMobileLayout(String userAgent) {
        assertThat(DeviceLayout.fromUserAgent(userAgent)).isEqualTo(DeviceLayout.MOBILE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/126.0 Safari/537.36",
            "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 14; SM-X710) AppleWebKit/537.36 Chrome/126.0 Safari/537.36",
            "Mozilla/5.0 (Android 14; Tablet; rv:128.0) Gecko/128.0 Firefox/128.0"})
    void desktopsAndTabletsKeepTheRegularLayout(String userAgent) {
        assertThat(DeviceLayout.fromUserAgent(userAgent)).isEqualTo(DeviceLayout.DESKTOP);
    }

    @Test
    void missingUserAgentFallsBackToDesktop() {
        assertThat(DeviceLayout.fromUserAgent(null)).isEqualTo(DeviceLayout.DESKTOP);
    }

    @Test
    void parameterOverridesAreCaseInsensitiveAndIgnoreUnknownValues() {
        assertThat(DeviceLayout.fromParameter(" Mobile ")).contains(DeviceLayout.MOBILE);
        assertThat(DeviceLayout.fromParameter("DESKTOP")).contains(DeviceLayout.DESKTOP);
        assertThat(DeviceLayout.fromParameter("tablet")).isEmpty();
        assertThat(DeviceLayout.fromParameter(null)).isEmpty();
    }
}
