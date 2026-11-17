package de.zettsystems.starfare.auth.ui;

import de.zettsystems.gamesplatformapi.PlatformAccounts;
import org.springframework.stereotype.Component;

/** Hands the platform's accounts to {@link UserContext} once the application context starts. */
@Component
class UserContextBinding {

    UserContextBinding(PlatformAccounts accounts) {
        UserContext.bind(accounts);
    }
}
