package example;

import de.zettsystems.identity.application.IdentityUserDetails;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.ui.LobbyView;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetRef;
import tools.jackson.databind.json.JsonMapper;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/** An independent consumer: no source checkout, includeBuild or extra runtime dependencies. */
public final class ModuleConsumer {
    private ModuleConsumer() { }

    public static void main(String[] args) {
        require(GameState.class.getProtectionDomain().getCodeSource().getLocation().getPath().endsWith(".jar"),
                "Game classes must come from a published archive");
        var forbidden = List.of("vaadin-dev-", "spring-boot-docker-compose-", "junit-",
                "testcontainers-", "cucumber-", "mockito-");
        for (var entry : System.getProperty("java.class.path").split(Pattern.quote(File.pathSeparator))) {
            var file = Path.of(entry).getFileName().toString();
            require(forbidden.stream().noneMatch(file::startsWith), "Non-production dependency: " + file);
        }
        var catalog = RulesetCatalog.builtIn();
        require(catalog.definitions().size() == 2, "Both game variants must be available");
        catalog.requireCreatable(RulesetRef.SECTOR_FORCES);
        catalog.requireCreatable(RulesetCatalog.ECONOMY_CANDIDATE);
        var state = new GameState();
        state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.SECTOR_FORCES));
        state.start();
        var mapper = JsonMapper.builder().findAndAddModules().build();
        var json = mapper.writeValueAsString(GameState.toSnapshot(state));
        var restored = GameState.fromSnapshot(mapper.readValue(json, GameStateSnapshot.class));
        require(restored.ruleset().equals(RulesetRef.SECTOR_FORCES), "Snapshot round trip lost the ruleset");
        require(restored.started(), "Snapshot round trip lost the started state");
        require(GameService.class.isInterface(), "Game service API unavailable");
        require(LobbyView.class.getName().endsWith("LobbyView"), "Vaadin views unavailable");
        require(IdentityUserDetails.class.getName().endsWith("IdentityUserDetails"), "Identity dependency unavailable");
        var loader = ModuleConsumer.class.getClassLoader();
        for (var resource : new String[]{"META-INF/frontend/battle-replay.js", "META-INF/frontend/map-geometry.js",
                "META-INF/frontend/styles/starfare.css", "META-INF/resources/icons/starfare.svg",
                "games/starfare/migrations/V2_1__create_schema.sql", "vaadin-i18n/translations.properties"}) {
            require(loader.getResource(resource) != null, "Missing resource: " + resource);
        }
        require(loader.getResource("de/zettsystems/starfare/StarfareApplication.class") == null,
                "Executable application leaked into game module");
        require(loader.getResource("de/zettsystems/starfare/AppShell.class") == null,
                "Standalone AppShell leaked into game module");
        System.out.println("Published module consumed: both variants, snapshot round trip, views, identity and resources verified.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) { throw new IllegalStateException(message); }
    }
}
