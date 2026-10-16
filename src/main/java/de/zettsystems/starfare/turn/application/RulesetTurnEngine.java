package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Dispatches all human and automatic round advancement by the complete stored reference. Rounds
 * resolving slower than {@code starfare.timing.slow-round-ms} ({@code 0} disables it) are logged at
 * WARN for {@code prod-langsam.sh}.
 */
@Service
@Primary
public final class RulesetTurnEngine implements TurnEngine {
    private static final Logger LOG = LoggerFactory.getLogger(RulesetTurnEngine.class);
    private static final long DEFAULT_SLOW_ROUND_MILLIS = 1000;

    private final RulesetCatalog catalog;
    private final Map<RulesetRef, TurnEngine> engines;
    private final long slowRoundMillis;

    @Autowired
    public RulesetTurnEngine(RulesetCatalog catalog, List<RulesetRoundImplementation> implementations,
                             @Value("${starfare.timing.slow-round-ms:1000}") long slowRoundMillis) {
        this(catalog, implementations.stream().collect(Collectors.toMap(
                RulesetRoundImplementation::ruleset, Function.identity())), slowRoundMillis);
    }

    RulesetTurnEngine(RulesetCatalog catalog, List<RulesetRoundImplementation> implementations) {
        this(catalog, implementations, DEFAULT_SLOW_ROUND_MILLIS);
    }

    RulesetTurnEngine(RulesetCatalog catalog, Map<RulesetRef, TurnEngine> engines) {
        this(catalog, engines, DEFAULT_SLOW_ROUND_MILLIS);
    }

    private RulesetTurnEngine(RulesetCatalog catalog, Map<RulesetRef, TurnEngine> engines, long slowRoundMillis) {
        this.catalog = catalog;
        this.engines = Map.copyOf(engines);
        this.slowRoundMillis = slowRoundMillis;
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
        int round = state.turn();
        long start = System.nanoTime();
        engine.advanceTurn(state);
        long millis = (System.nanoTime() - start) / 1_000_000;
        if (slowRoundMillis > 0 && millis >= slowRoundMillis && LOG.isWarnEnabled()) {
            LOG.warn("Langsame Runde „{}/{}“: Runde {}, {} ms, {} Imperien, {} Systeme",
                    state.ruleset().variant(), state.ruleset().version(), round, millis,
                    state.players().size(), state.systems().size());
        }
    }
}
