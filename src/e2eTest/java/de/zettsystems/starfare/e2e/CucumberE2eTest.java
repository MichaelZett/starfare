package de.zettsystems.starfare.e2e;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Runs the browser journeys from {@code src/e2eTest/resources/features/}.
 *
 * <p>Keeps only what needs a real browser: sign-in and a first round, push between two
 * sessions, battle playback with audio, dragging, hit areas, keyboard use, touch on the
 * phone layout, route geometry and layout screenshots. Journeys that need no JavaScript run
 * as Vaadin Browserless tests under {@code src/test}, together with the unit and
 * integration tests.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "de.zettsystems.starfare.e2e")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
public class CucumberE2eTest {
}
