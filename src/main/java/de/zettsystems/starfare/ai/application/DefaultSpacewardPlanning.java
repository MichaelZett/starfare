package de.zettsystems.starfare.ai.application;

import de.zettsystems.starfare.diplomacy.application.DefaultDiplomacyService;
import de.zettsystems.starfare.diplomacy.application.DiplomacyService;
import de.zettsystems.starfare.economy.application.EconomyService;
import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.game.application.PlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.game.values.VisibleSystem;
import de.zettsystems.starfare.navigation.domain.Routes;
import java.util.Comparator;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Uses the same filtered system information available to a human player. */
@Service
public class DefaultSpacewardPlanning implements SpacewardPlanning {
    private final ProfilePlanning profiles;
    private final EconomyService economy;
    private final PlayerViewBuilder views;
    private final DiplomacyService diplomacy;
    public DefaultSpacewardPlanning(EconomyService economy, PlayerViewBuilder views) {
        this(economy, views, new DefaultDiplomacyService());
    }
    public DefaultSpacewardPlanning(EconomyService economy, PlayerViewBuilder views, DiplomacyService diplomacy) {
        this(economy, views, diplomacy, new DefaultProfilePlanning(views, economy));
    }
    @org.springframework.beans.factory.annotation.Autowired
    public DefaultSpacewardPlanning(EconomyService economy, PlayerViewBuilder views, DiplomacyService diplomacy,
                                    ProfilePlanning profiles) {
        this.profiles = profiles;
        this.diplomacy = diplomacy;
        this.economy = economy;
        this.views = views;
    }
    @Override
    public void plan(GameState state, FleetService fleets) {
        diplomacy.answerAiProposals(state);
        state.players().stream().filter(Player::ai).forEach(player -> planPlayer(state, fleets, player));
    }
    private void planPlayer(GameState state, FleetService fleets, Player player) {
        if (player.aiStrategy() != de.zettsystems.starfare.ai.values.AiStrategy.BASELINE) {
            profiles.plan(state, fleets, player);
            return;
        }
        PlayerViewState view = views.forPlayer(state, player.id());
        var owned = view.systems().stream().filter(s -> Objects.equals(s.ownerId(), player.id()) && s.fullyVisible()).toList();
        for (VisibleSystem system : owned) {
            var industry = system.industry();
            if (industry == null) { continue; }
            boolean threatened = view.systems().stream().anyMatch(contact -> contact.ownerId() != null
                    && !Objects.equals(contact.ownerId(), player.id()) && !state.allied(player.id(), contact.ownerId())
                    && state.travelRounds(system.id(), contact.id()) <= 2);
            int allocation = threatened || industry.atMaximum() ? 0 : industry.usableCapacity() / 3;
            economy.allocateExpansion(state, player.id(), system.id(), allocation);
        }
        owned.stream().max(Comparator.comparingInt(DefaultSpacewardPlanning::knownGarrison))
                .ifPresent(base -> sendFrom(state, fleets, player, base, view));
    }
    private static int knownGarrison(VisibleSystem system) {
        Integer garrison = system.garrison();
        return garrison == null ? 0 : garrison;
    }

    private static void sendFrom(GameState state, FleetService fleets, Player player,
                                 VisibleSystem base, PlayerViewState view) {
        var system = state.getSystem(base.id());
        int available = Math.max(0, system.availableShips() - Math.max(1, system.productionPerTurn()));
        if (available == 0) { return; }
        view.systems().stream().filter(s -> !Objects.equals(s.ownerId(), player.id()) && !state.allied(player.id(), s.ownerId()))
                .filter(target -> Routes.plan(state, player.id(), base.id(), target.id()).isPresent())
                .min(Comparator.comparingDouble(s -> state.distance(base.id(), s.id())))
                .ifPresent(target -> fleets.queueSend(state, player.id(), base.id(), target.id(), Math.max(1, available / 2)));
    }
}
