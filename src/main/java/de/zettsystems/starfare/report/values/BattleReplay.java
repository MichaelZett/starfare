package de.zettsystems.starfare.report.values;

import java.util.Optional;

/** Combat-only presentation data. Ownership changes are deliberately kept outside the replay. */
public record BattleReplay(String systemName, int attacking, int defending, Outcome outcome,
                           double attackerStrength, double defenderStrength, java.util.List<CoalitionSide> coalitions,
                           @org.jspecify.annotations.Nullable Integer defenderSideId) {
    public BattleReplay { coalitions = java.util.List.copyOf(coalitions); }
    public BattleReplay(String systemName, int attacking, int defending, Outcome outcome,
                        double attackerStrength, double defenderStrength, java.util.List<CoalitionSide> coalitions) {
        this(systemName, attacking, defending, outcome, attackerStrength, defenderStrength, coalitions, null);
    }
    public BattleReplay(String systemName, int attacking, int defending, Outcome outcome,
                        double attackerStrength, double defenderStrength) {
        this(systemName, attacking, defending, outcome, attackerStrength, defenderStrength, java.util.List.of());
    }

    public record Outcome(int attackingRemaining, int defendingRemaining, boolean attackerWon) {}

    public BattleReplay(String systemName, int attacking, int defending, int attackingRemaining,
                        int defendingRemaining) {
        this(systemName, attacking, defending, attackingRemaining, defendingRemaining, attacking, defending);
    }

    public BattleReplay(String systemName, int attacking, int defending, int attackingRemaining,
                        int defendingRemaining, double attackerStrength, double defenderStrength) {
        this(systemName, attacking, defending, new Outcome(attackingRemaining, defendingRemaining,
                attackingRemaining > defendingRemaining
                        || (attackingRemaining == 0 && defendingRemaining == 0 && attackerStrength > defenderStrength)),
                attackerStrength, defenderStrength);
    }

    public int attackingRemaining() {
        return outcome.attackingRemaining();
    }

    public int defendingRemaining() {
        return outcome.defendingRemaining();
    }

    public boolean attackerWon() {
        return outcome.attackerWon();
    }

    public static Optional<BattleReplay> from(TurnEvent event) {
        return switch (event) {
            case TurnEvent.CoalitionBattle battle -> Optional.of(coalitionReplay(battle));
            case TurnEvent.BattleWon battle -> Optional.of(new BattleReplay(battle.systemName(), battle.attacking(),
                    battle.defending(), new Outcome(battle.remaining(), 0, true),
                    battle.attackerStrength(), battle.defenderStrength()));
            case TurnEvent.BattleLost battle -> Optional.of(new BattleReplay(battle.systemName(), battle.attacking(),
                    battle.defending(), new Outcome(0, battle.defendersLeft(), false),
                    battle.attackerStrength(), battle.defenderStrength()));
            case TurnEvent.SystemLost lost -> lost.attacking() > 0
                    ? Optional.of(new BattleReplay(lost.systemName(), lost.attacking(), lost.defending(),
                    new Outcome(lost.attackersRemaining(), 0, true),
                    lost.attackerStrength(), lost.defenderStrength())) : Optional.empty();
            case TurnEvent.DefenseHeld held -> held.attacking() > 0
                    ? Optional.of(new BattleReplay(held.systemName(), held.attacking(), held.defending(),
                    new Outcome(0, held.defendersLeft(), false),
                    held.attackerStrength(), held.defenderStrength())) : Optional.empty();
            case TurnEvent.Recycling _, TurnEvent.Production _, TurnEvent.Reinforcement _, TurnEvent.Victory _, TurnEvent.Defeat _,
                 TurnEvent.EconomySummary _, TurnEvent.ResearchCompleted _ -> Optional.empty();
        };
    }

    private static BattleReplay coalitionReplay(TurnEvent.CoalitionBattle battle) {
        Integer owner = battle.previousOwner();
        int defender = owner == null ? 0 : owner;
        Integer sideId = battle.sides().stream().filter(side -> side.contains(defender))
                .map(CoalitionSide::id).findFirst().orElse(null);
        return new BattleReplay(battle.systemName(), 0, 0, new Outcome(0, 0, false), 0, 0, battle.sides(), sideId);
    }
}
