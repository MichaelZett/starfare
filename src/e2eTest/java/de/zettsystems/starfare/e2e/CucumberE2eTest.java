package de.zettsystems.starfare.e2e;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Startet die Cucumber-Suite aus {@code src/e2eTest/resources/features/}.
 *
 * <p>Bewusst nur ein Happy-Path-Smoke: Konto anlegen, bestätigen, anmelden,
 * Spiel anlegen, starten, eine Runde spielen. Die Spiellogik selbst bleibt in
 * den Unit-/Integrationstests unter {@code src/test}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "de.zettsystems.starfare.e2e")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
public class CucumberE2eTest {
}
