package de.zettsystems.starfare.standalone.platform;

import de.zettsystems.gamesplatformapi.PlatformSettings;
import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.testsupport.TestPlatformAccounts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class SoloPlatformSettingsTest extends AbstractIntegrationTest {
    @Autowired
    private PlatformSettings settings;
    @Autowired
    private TestPlatformAccounts accounts;

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void keepsTheValuesOfTheSignedInAccount() {
        signIn("settings-alice");
        settings.set("starfare", "SPEED", "0.0");
        settings.set("starfare", "SOUND", "false");
        settings.set("starfare", "SPEED", "1.5");
        signIn("settings-bob");
        settings.set("starfare", "SPEED", "2.0");

        signIn("settings-alice");
        assertThat(settings.values("starfare", List.of("SPEED", "SOUND", "SIDEBAR")))
                .containsExactly(java.util.Map.entry("SPEED", "1.5"), java.util.Map.entry("SOUND", "false"));
        settings.reset("starfare", "SPEED");
        assertThat(settings.value("starfare", "SPEED")).as("the game's default applies again").isEmpty();
        assertThat(settings.value("starfare", "SOUND")).contains("false");
    }

    @Test
    void readsNothingWithoutAnAccountOrForAnotherGame() {
        assertThat(settings.value("starfare", "SPEED")).isEmpty();
        assertThatIllegalStateException().isThrownBy(() -> settings.set("starfare", "SPEED", "1.0"));
        signIn("settings-carol");
        settings.set("starfare", "SPEED", "1.0");

        assertThat(settings.value("frontier-lords", "SPEED")).isEmpty();
        assertThatIllegalArgumentException().isThrownBy(() -> settings.set("frontier-lords", "SPEED", "1.0"));
    }

    @Test
    void rejectsBlankAndTooLongKeysAndValues() {
        signIn("settings-dave");

        assertThatIllegalArgumentException().isThrownBy(() -> settings.set("starfare", " ", "1.0"));
        assertThatIllegalArgumentException().isThrownBy(() -> settings.set("starfare", "SPEED", " "));
        assertThatIllegalArgumentException().isThrownBy(() -> settings.set("starfare", "x".repeat(41), "1.0"));
        assertThatIllegalArgumentException().isThrownBy(() -> settings.set("starfare", "SPEED", "x".repeat(513)));
        settings.set("starfare", "SIDEBAR", "x".repeat(512));
        assertThat(settings.value("starfare", "SIDEBAR")).hasValue("x".repeat(512));
    }

    private final String run = UUID.randomUUID().toString();

    /** Signs in the account of this name, the same one again on every call within a test run. */
    private void signIn(String name) {
        String email = name + "-" + run + "@example.test";
        accounts.createAccount(email, name);
        SecurityContextHolder.setContext(accounts.signedIn(email));
    }
}
