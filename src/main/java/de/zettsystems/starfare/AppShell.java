package de.zettsystems.starfare;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Enables Vaadin server-push and loads the Aura theme. Forced dark color-scheme
 * is applied via starfare.css so Aura's light-dark() values resolve to dark.
 */
@Push
@CssImport("@vaadin/aura/aura.css")
@SuppressFBWarnings(value = "SE_NO_SERIALVERSIONID",
        justification = "Vaadin AppShell singleton; never crosses JVM boundaries, so serialVersionUID is irrelevant.")
public class AppShell implements AppShellConfigurator {
}
