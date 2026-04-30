package de.zettsystems.starfare.fleet.values;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A pending fleet command queued by a player, applied at the start of the next turn advance.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FleetOrder.Send.class, name = "send"),
        @JsonSubTypes.Type(value = FleetOrder.Wait.class, name = "wait"),
        @JsonSubTypes.Type(value = FleetOrder.Disband.class, name = "disband")
})
public sealed interface FleetOrder {

    int ownerId();

    record Send(int ownerId, int fromSystemId, int toSystemId, int ships) implements FleetOrder {}

    record Wait(int ownerId, int fleetId) implements FleetOrder {}

    record Disband(int ownerId, int fleetId) implements FleetOrder {}
}
