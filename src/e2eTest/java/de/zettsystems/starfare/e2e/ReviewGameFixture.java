package de.zettsystems.starfare.e2e;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.game.values.StarSystem;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Stable map geometry and production values for browser regression journeys. */
@Component
public class ReviewGameFixture {
    public record Game(GameId id, int hostSeat, int guestSeat) {}

    private final GameService games;
    private final GameRegistry registry;
    private final UserAccountService accounts;

    ReviewGameFixture(GameService games, GameRegistry registry, UserAccountService accounts) {
        this.games = games;
        this.registry = registry;
        this.accounts = accounts;
    }

    public Game create(String hostEmail, @Nullable String guestEmail) {
        GameSetup defaults = GameSetup.defaults();
        GameSetup setup = new GameSetup(8, guestEmail == null ? 1 : 2, 0, List.of(20, 2),
                1, 2, 5, true, true, defaults.seatColorHexes(), defaults.productionDistribution(),
                defaults.galaxyLayout(), true, defaults.roundRules());
        String host = accountId(hostEmail);
        GameId id = games.newGame(setup, host, "Review regression");
        if (guestEmail != null) {
            assertThat(games.changeVisibility(id, host, GameVisibility.PUBLIC)).isTrue();
        }
        int hostSeat = games.joinGame(id, host, "Review Host", "Host Empire").orElseThrow();
        int guestSeat = guestEmail == null ? -1
                : games.joinGame(id, accountId(guestEmail), "Review Guest", "Guest Empire").orElseThrow();
        registry.writeState(id, state -> {
            state.systems().clear();
            state.systems().add(new StarSystem(1, "Alpha", 400, 350, hostSeat, 5, 20, false));
            state.systems().add(new StarSystem(2, "Beta", 800, 350, hostSeat, 3, 2, false));
            state.systems().add(new StarSystem(3, "Gamma", 2400, 1400,
                    guestSeat < 0 ? null : guestSeat, 5, 2, guestSeat < 0));
            for (int system = 4; system <= 8; system++) {
                state.systems().add(new StarSystem(system, "Remote " + system,
                        2000 + system * 80, 1300 + system * 50, null, 2, 1, true));
            }
            return null;
        });
        assertThat(games.startGame(id)).isTrue();
        return new Game(id, hostSeat, guestSeat);
    }

    private String accountId(String email) {
        return String.valueOf(accounts.findByEmail(email).orElseThrow().id());
    }
}
