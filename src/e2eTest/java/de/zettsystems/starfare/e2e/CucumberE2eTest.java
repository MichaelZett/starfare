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
 * <p>Exercises account registration, lobby access, rounds, battle playback,
 * map interactions and multiplayer chat and continuation. Core game logic is
 * covered by unit and integration tests under {@code src/test}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "de.zettsystems.starfare.e2e")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
public class CucumberE2eTest {
}
