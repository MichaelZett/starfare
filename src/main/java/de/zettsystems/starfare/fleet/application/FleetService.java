package de.zettsystems.starfare.fleet.application;

import de.zettsystems.starfare.game.domain.GameState;

import java.util.Set;

public interface FleetService {

    boolean queueSend(GameState state, int playerId, int fromId, int toId, int ships);

    boolean queueWait(GameState state, int playerId, int fleetId);

    boolean queueDisband(GameState state, int playerId, int fleetId);

    boolean sendFleet(GameState state, int playerId, int fromId, int toId, int ships);

    boolean setFleetWait(GameState state, int playerId, int fleetId);

    boolean disbandFleet(GameState state, int playerId, int fleetId);

    int addStandingOrder(GameState state, int playerId, int fromId, int toId);

    boolean removeStandingOrder(GameState state, int playerId, int orderId);

    boolean removeStandingOrderFrom(GameState state, int playerId, int fromSystemId);

    Set<Integer> applyStandingOrdersForProduction(GameState state);
}
