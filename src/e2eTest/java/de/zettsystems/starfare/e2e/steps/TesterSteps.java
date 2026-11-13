package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.identity.values.Scope;
import de.zettsystems.starfare.game.config.StandaloneStart;
import io.cucumber.java.de.Wenn;

/**
 * Testvarianten wie Spaceward stehen in einem Plattform-Host nur Testern offen. Szenarien, die
 * sie im Wizard brauchen, geben dem Konto deshalb ausdrücklich die Testerrolle für Starfare. Im
 * eigenen Start ist ohnehin alles frei, und die Rolle gibt es dort nicht; der Schritt tut dann nichts.
 */
public class TesterSteps {
    private final UserAccountService accounts;

    public TesterSteps(UserAccountService accounts) {
        this.accounts = accounts;
    }

    @Wenn("das Konto {string} Tester für Starfare ist")
    public void grantStarfareTester(String email) {
        if (StandaloneStart.active()) {
            return;
        }
        long id = accounts.findByEmail(email).orElseThrow().id();
        accounts.grantRole(id, "GAME_TESTER", Scope.of("game", "starfare"));
    }
}
