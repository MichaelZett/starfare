package de.zettsystems.starfare.e2e;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

/** Der Zustand eines Szenarios; je Szenario eine frische Instanz. */
@Component
@ScenarioScope
public class E2eWorld {

    private String email;
    private String displayName;

    public void rememberAccount(String displayName, String email) {
        this.displayName = displayName;
        this.email = email;
    }

    public String email() {
        if (email == null) {
            throw new IllegalStateException("Im Szenario wurde noch kein Konto angelegt.");
        }
        return email;
    }

    public String displayName() {
        if (displayName == null) {
            throw new IllegalStateException("Im Szenario wurde noch kein Konto angelegt.");
        }
        return displayName;
    }
}
