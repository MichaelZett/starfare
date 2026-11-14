package de.zettsystems.starfare;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Enables Vaadin server-push and loads the Aura theme and the platform stylesheet (a checked
 * copy of games-platform's platform.css, see syncPlatformStyles). index.html marks the page as
 * Starfare's dark game area, so Aura's light-dark() values resolve to dark. The platform's UI
 * font DM Sans is bundled from npm, as in the host.
 */
@Push
@NpmPackage(value = "@fontsource-variable/dm-sans", version = "5.3.0")
@CssImport("@vaadin/aura/aura.css")
@CssImport("@fontsource-variable/dm-sans/opsz.css")
@CssImport("./styles/platform.css")
@SuppressFBWarnings(value = "SE_NO_SERIALVERSIONID",
        justification = "Vaadin AppShell singleton; never crosses JVM boundaries, so serialVersionUID is irrelevant.")
public class AppShell implements AppShellConfigurator {
    private static final String FAVICON_PATH = "icons/starfare.svg";

    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addFavIcon("icon", FAVICON_PATH, "any");
    }
}
