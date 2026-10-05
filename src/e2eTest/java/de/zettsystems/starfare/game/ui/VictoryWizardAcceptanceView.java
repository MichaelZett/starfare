package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.*;
import jakarta.annotation.security.PermitAll;

/** Test-only preview of an unreleased wizard; never exposes game creation in production. */
@Route("acceptance/victory-wizard")
@PermitAll
public class VictoryWizardAcceptanceView extends VerticalLayout {
    public VictoryWizardAcceptanceView(GameService game, PlayerDirectory directory) {
        var catalog = new RulesetCatalog(game.rulesets().definitions().stream().map(entry ->
                new RulesetDefinition(entry.defaultRef(), entry.nameKey(), entry.descriptionKey(),
                        entry.supportedVersions(), true)).toList());
        add(new Button("Wizard", _ -> CreateGameWizardDialog.open(game, directory, () -> { }, catalog)));
    }
}
