package de.zettsystems.starfare.fleet.application;

import de.zettsystems.starfare.game.domain.GameState;

import java.util.Map;
import org.jspecify.annotations.Nullable;

public interface FleetService {

    boolean queueSend(GameState state, int playerId, int fromId, int toId, int ships);

    default boolean queueSend(GameState state, int playerId, int fromId, int toId, int ships,
                              @Nullable Integer beneficiary) {
        return queueSend(state, playerId, fromId, toId, ships);
    }
    default boolean sendFleet(GameState state, int playerId, int fromId, int toId, int ships,
                              @Nullable Integer beneficiary) {
        return sendFleet(state, playerId, fromId, toId, ships);
    }

    boolean queueWait(GameState state, int playerId, int fleetId);

    boolean queueDisband(GameState state, int playerId, int fleetId);

    boolean sendFleet(GameState state, int playerId, int fromId, int toId, int ships);

    boolean setFleetWait(GameState state, int playerId, int fleetId);

    boolean disbandFleet(GameState state, int playerId, int fleetId);

    int addStandingOrder(GameState state, int playerId, int fromId, int toId, int ships);

    /** Obergrenze fuer alle ausgehenden Verlegungen: eigene Produktion plus eingehende. */
    int routingCapacity(GameState state, int playerId, int systemId);

    /** Summe der bereits von diesem System ausgehenden Verlegungen. */
    int routedFrom(GameState state, int playerId, int systemId);

    /** Groesse, die eine neue oder ersetzte Verlegung {@code fromId → toId} hoechstens haben darf. */
    int routingHeadroom(GameState state, int playerId, int fromId, int toId);

    boolean removeStandingOrder(GameState state, int playerId, int orderId);

    boolean removeStandingOrderFrom(GameState state, int playerId, int fromSystemId);

    /** Fuehrt alle Verlegungen aus und meldet je Quellsystem, wie viel abgeflossen ist. */
    Map<Integer, Integer> applyStandingOrdersForProduction(GameState state);
}
