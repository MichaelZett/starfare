package de.zettsystems.starfare.report.values;

import java.util.Optional;

/** Combat-only presentation data. Ownership changes are deliberately kept outside the replay. */
public record BattleReplay(String systemName, int attacking, int defending, int attackingRemaining,
                           int defendingRemaining) {

    public static Optional<BattleReplay> from(TurnEvent event) {
        return switch (event) {
            case TurnEvent.BattleWon battle -> Optional.of(new BattleReplay(battle.systemName(), battle.attacking(),
                    battle.defending(), battle.remaining(), 0));
            case TurnEvent.BattleLost battle -> Optional.of(new BattleReplay(battle.systemName(), battle.attacking(),
                    battle.defending(), 0, battle.defendersLeft()));
            case TurnEvent.Production _, TurnEvent.Reinforcement _, TurnEvent.SystemLost _,
                    TurnEvent.DefenseHeld _, TurnEvent.Victory _, TurnEvent.Defeat _ -> Optional.empty();
        };
    }
}
