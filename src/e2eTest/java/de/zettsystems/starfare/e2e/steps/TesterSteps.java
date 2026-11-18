package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.starfare.testsupport.TestPlatformAccounts;
import io.cucumber.java.de.Wenn;

/**
 * Testvarianten wie Spaceward stehen in einem Plattform-Host nur Testern offen. Szenarien, die
 * sie im Wizard brauchen, geben dem Konto deshalb ausdrücklich die Testerrolle für Starfare. Im
 * eigenen Start ist ohnehin alles frei; die Rolle schadet dort nicht.
 */
public class TesterSteps {
    private final TestPlatformAccounts accounts;

    public TesterSteps(TestPlatformAccounts accounts) {
        this.accounts = accounts;
    }

    @Wenn("das Konto {string} Tester für Starfare ist")
    public void grantStarfareTester(String email) {
        accounts.grantTester(email);
    }
}
