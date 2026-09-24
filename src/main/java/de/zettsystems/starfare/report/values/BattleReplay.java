package de.zettsystems.starfare.report.values;

import java.util.Optional;

/** Combat-only presentation data. Ownership changes are deliberately kept outside the replay. */
public record BattleReplay(String systemName, int attacking, int defending, int attackingRemaining,
                           int defendingRemaining, double attackerStrength, double defenderStrength) {

    public BattleReplay(String systemName, int attacking, int defending, int attackingRemaining,
                        int defendingRemaining) {
        this(systemName, attacking, defending, attackingRemaining, defendingRemaining, attacking, defending);
    }

    public static Optional<BattleReplay> from(TurnEvent event) {
        return switch (event) {
            case TurnEvent.BattleWon battle -> Optional.of(new BattleReplay(battle.systemName(), battle.attacking(),
                    battle.defending(), battle.remaining(), 0, battle.attackerStrength(), battle.defenderStrength()));
            case TurnEvent.BattleLost battle -> Optional.of(new BattleReplay(battle.systemName(), battle.attacking(),
                    battle.defending(), 0, battle.defendersLeft(), battle.attackerStrength(), battle.defenderStrength()));
            case TurnEvent.SystemLost lost -> lost.attacking() > 0
                    ? Optional.of(new BattleReplay(lost.systemName(), lost.attacking(), lost.defending(),
                    lost.attackersRemaining(), 0, lost.attackerStrength(), lost.defenderStrength())) : Optional.empty();
            case TurnEvent.DefenseHeld held -> held.attacking() > 0
                    ? Optional.of(new BattleReplay(held.systemName(), held.attacking(), held.defending(),
                    0, held.defendersLeft(), held.attackerStrength(), held.defenderStrength())) : Optional.empty();
            case TurnEvent.Production _, TurnEvent.Reinforcement _, TurnEvent.Victory _, TurnEvent.Defeat _ ->
                    Optional.empty();
        };
    }
}
