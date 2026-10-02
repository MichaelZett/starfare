package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Dispatches all human and automatic round advancement by the complete stored reference. */
@Service
@Primary
public final class RulesetTurnEngine implements TurnEngine {
    private final RulesetCatalog catalog;
    private final Map<RulesetRef, TurnEngine> engines;

    @Autowired
    public RulesetTurnEngine(RulesetCatalog catalog, List<RulesetRoundImplementation> implementations) {
        this(catalog, implementations.stream().collect(Collectors.toMap(
                RulesetRoundImplementation::ruleset, Function.identity())));
    }

    RulesetTurnEngine(RulesetCatalog catalog, Map<RulesetRef, TurnEngine> engines) {
        this.catalog = catalog;
        this.engines = Map.copyOf(engines);
        for (var entry : catalog.definitions()) {
            for (String version : entry.supportedVersions()) {
                if (!engines.containsKey(new RulesetRef(entry.defaultRef().variant(), version))) {
                    throw new IllegalArgumentException("Missing round implementation for " + entry.defaultRef().variant() + " / " + version);
                }
            }
        }
    }

    @Override
    public void advanceTurn(GameState state) {
        catalog.requireSupported(state.ruleset());
        TurnEngine engine = engines.get(state.ruleset());
        if (engine == null) {
            throw new IllegalArgumentException("No round implementation for ruleset " + state.ruleset());
        }
        engine.advanceTurn(state);
    }
}
