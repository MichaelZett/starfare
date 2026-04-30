package de.zettsystems.starfare.game.ui;

/**
 * Central registry of i18n keys. Resolve via Component#getTranslation or {@link de.zettsystems.starfare.i18n.I18n}.
 */
public final class UiTexts {
    // Lobby — header / toolbar / empty state
    public static final String LOBBY_HEADER_TITLE = "lobby.header.title";
    public static final String LOBBY_SUBTITLE = "lobby.subtitle";
    public static final String LOBBY_EMPTY_TITLE = "lobby.empty.title";
    public static final String LOBBY_EMPTY_BODY = "lobby.empty.body";
    public static final String LOBBY_NEW_GAME = "lobby.newGame";
    public static final String LOBBY_JOINED = "lobby.joined";
    public static final String LOBBY_JOIN_FAILED = "lobby.joinFailed";
    public static final String LOBBY_START_FAILED = "lobby.startFailed";

    // Lobby — grid
    public static final String LOBBY_COLUMN_GAME = "lobby.column.game";
    public static final String LOBBY_COLUMN_TURN = "lobby.column.turn";
    public static final String LOBBY_COLUMN_PLAYERS = "lobby.column.players";
    public static final String LOBBY_COLUMN_STATUS = "lobby.column.status";
    public static final String LOBBY_COLUMN_ACTIONS = "lobby.column.actions";
    public static final String LOBBY_TURN_LABEL = "lobby.turnLabel";

    // Lobby — row actions
    public static final String LOBBY_ACTION_JOIN = "lobby.action.join";
    public static final String LOBBY_ACTION_START = "lobby.action.start";
    public static final String LOBBY_ACTION_PLAY = "lobby.action.play";
    public static final String LOBBY_ACTION_ABORT = "lobby.action.abort";
    public static final String LOBBY_ACTION_OBSERVE = "lobby.action.observe";
    public static final String LOBBY_OBSERVE_FAILED = "lobby.observeFailed";
    public static final String LOBBY_OBSERVING = "lobby.observing";

    // Status badges
    public static final String STATUS_CREATED = "status.created";
    public static final String STATUS_WAITING = "status.waiting";
    public static final String STATUS_RUNNING = "status.running";
    public static final String STATUS_FINISHED = "status.finished";

    // Wizard
    public static final String LOBBY_WIZARD_TITLE = "lobby.wizard.title";
    public static final String LOBBY_WIZARD_INTRO = "lobby.wizard.intro";
    public static final String LOBBY_WIZARD_SECTION_SETUP = "lobby.wizard.section.setup";
    public static final String LOBBY_WIZARD_SECTION_SETUP_HINT = "lobby.wizard.section.setup.hint";
    public static final String LOBBY_WIZARD_SECTION_NEUTRAL = "lobby.wizard.section.neutral";
    public static final String LOBBY_WIZARD_SECTION_NEUTRAL_HINT = "lobby.wizard.section.neutral.hint";
    public static final String LOBBY_WIZARD_SECTION_START = "lobby.wizard.section.start";
    public static final String LOBBY_WIZARD_SECTION_START_HINT = "lobby.wizard.section.start.hint";
    public static final String LOBBY_WIZARD_CREATE = "lobby.wizard.create";
    public static final String LOBBY_WIZARD_CANCEL = "lobby.wizard.cancel";
    public static final String LOBBY_FIELD_SYSTEMS = "lobby.field.systems";
    public static final String LOBBY_FIELD_HUMANS = "lobby.field.humans";
    public static final String LOBBY_FIELD_AI = "lobby.field.ai";
    public static final String LOBBY_FIELD_COLOR = "lobby.field.color";
    public static final String LOBBY_FIELD_NEUTRAL_MIN_PRODUCTION = "lobby.field.neutralMinProduction";
    public static final String LOBBY_FIELD_NEUTRAL_MAX_PRODUCTION = "lobby.field.neutralMaxProduction";
    public static final String LOBBY_FIELD_START_PRODUCTION_HUMAN = "lobby.field.startProduction.human";
    public static final String LOBBY_FIELD_START_PRODUCTION_AI = "lobby.field.startProduction.ai";
    public static final String LOBBY_FIELD_START_GARRISON = "lobby.field.startGarrison";
    public static final String LOBBY_FIELD_OBSERVERS_ALLOWED = "lobby.field.observersAllowed";
    public static final String LOBBY_FIELD_REENTRY_ALLOWED = "lobby.field.reentryAllowed";

    // Map — header / submit / game-over
    public static final String MAP_HEADER_TITLE = "map.header.title";
    public static final String MAP_ROUND_LABEL = "map.roundLabel";
    public static final String MAP_NEXT_ROUND = "map.nextRound";
    public static final String MAP_SUBMIT_WAITING = "map.submit.waiting";
    public static final String MAP_SUBMIT_FAILED = "map.submit.failed";
    public static final String MAP_GAME_OVER = "map.gameOver";
    public static final String MAP_ACTION_LOBBY = "map.action.lobby";
    public static final String MAP_ACTION_LEAVE = "map.action.leave";
    public static final String MAP_ACTION_LEAVE_OBSERVE = "map.action.leaveObserve";
    public static final String MAP_LEAVE_FAILED = "map.leaveFailed";
    public static final String MAP_LEFT = "map.left";

    // Map — send-fleet dialog
    public static final String MAP_SEND_FLEET = "map.sendFleet";
    public static final String MAP_SHIPS_LABEL = "map.ships";
    public static final String MAP_DURATION_SINGULAR = "map.duration.singular";
    public static final String MAP_DURATION_PLURAL = "map.duration.plural";
    public static final String MAP_AVAILABLE = "map.available";
    public static final String MAP_SEND_QUICK_ALL = "map.send.quick.all";
    public static final String MAP_SEND_QUICK_HALF = "map.send.quick.half";
    public static final String MAP_SEND_QUICK_DOUBLE = "map.send.quick.double";
    public static final String MAP_STANDING_ORDER_CHECKBOX = "map.standingOrder.checkbox";
    public static final String MAP_INVALID_COMMAND = "map.invalidCommand";
    public static final String MAP_DIALOG_CANCEL = "map.dialog.cancel";
    public static final String MAP_ADD_STANDING_ORDER_FAILED = "map.addStandingOrder.failed";

    // Map — fleets grid
    public static final String MAP_OWN_FLEETS = "map.ownFleets";
    public static final String MAP_COLUMN_NO = "map.column.no";
    public static final String MAP_COLUMN_FROM = "map.column.from";
    public static final String MAP_COLUMN_TO = "map.column.to";
    public static final String MAP_COLUMN_SHIPS = "map.column.ships";
    public static final String MAP_COLUMN_ETA = "map.column.eta";
    public static final String MAP_ACTION_WAIT = "map.action.wait";
    public static final String MAP_ACTION_WAITING = "map.action.waiting";
    public static final String MAP_ACTION_DISBAND = "map.action.disband";
    public static final String MAP_WAIT_FAILED = "map.waitFailed";
    public static final String MAP_DISBAND_FAILED = "map.disbandFailed";

    // Map — planned orders grid
    public static final String MAP_PLANNED_ORDERS = "map.plannedOrders";
    public static final String MAP_COLUMN_ORDER_TYPE = "map.column.order.type";
    public static final String MAP_COLUMN_ORDER_FROM = "map.column.order.from";
    public static final String MAP_COLUMN_ORDER_TO = "map.column.order.to";
    public static final String MAP_COLUMN_ORDER_SHIPS = "map.column.order.ships";
    public static final String MAP_ACTION_CANCEL_ORDER = "map.action.cancelOrder";
    public static final String MAP_CANCEL_ORDER_FAILED = "map.cancelOrder.failed";

    // Map — standing orders
    public static final String MAP_STANDING_ORDERS_HEADER = "map.standingOrders.header";
    public static final String MAP_STANDING_ORDERS_MANAGE = "map.standingOrders.manage";
    public static final String MAP_STANDING_ORDERS_EMPTY = "map.standingOrders.empty";
    public static final String MAP_STANDING_ORDERS_HEADER_COUNT = "map.standingOrders.headerCount";
    public static final String MAP_STANDING_ORDERS_DIALOG_TITLE = "map.standingOrders.dialog.title";
    public static final String MAP_STANDING_ORDERS_DIALOG_CLOSE = "map.standingOrders.dialog.close";
    public static final String MAP_COLUMN_STANDING_FROM = "map.column.standing.from";
    public static final String MAP_COLUMN_STANDING_TO = "map.column.standing.to";
    public static final String MAP_COLUMN_STANDING_PRODUCTION = "map.column.standing.production";
    public static final String MAP_ACTION_DELETE_STANDING = "map.action.deleteStanding";
    public static final String MAP_REMOVE_STANDING_ORDER_FAILED = "map.removeStandingOrder.failed";
    public static final String MAP_SHOW_STANDING_TOGGLE = "map.showStanding.toggle";
    public static final String MAP_ORDER_TYPE_STANDING = "map.orderType.standing";

    // Map — fleet badge
    public static final String MAP_FLEET_BADGE_BASE = "map.fleetBadge.base";
    public static final String MAP_FLEET_BADGE_ETA_THIS_TURN = "map.fleetBadge.etaThisTurn";
    public static final String MAP_FLEET_BADGE_ETA_IN_SINGULAR = "map.fleetBadge.etaInSingular";
    public static final String MAP_FLEET_BADGE_ETA_IN_PLURAL = "map.fleetBadge.etaInPlural";

    // Map — stats
    public static final String MAP_STATS_SYSTEMS_TOOLTIP = "map.stats.systems.tooltip";
    public static final String MAP_STATS_PRODUCTION_TOOLTIP = "map.stats.production.tooltip";
    public static final String MAP_STATS_SHIPS_TOOLTIP = "map.stats.ships.tooltip";

    // Map — tooltip
    public static final String MAP_TOOLTIP_LIVE = "map.tooltip.live";
    public static final String MAP_TOOLTIP_LAST_SEEN = "map.tooltip.lastSeen";
    public static final String MAP_TOOLTIP_NO_SIGHT = "map.tooltip.noSight";

    // Round view
    public static final String ROUND_HEADER_TITLE = "round.header.title";
    public static final String ROUND_NO_EVENTS = "round.noEvents";
    public static final String ROUND_NO_EVENTS_FILTERED = "round.noEventsFiltered";
    public static final String ROUND_REPORT_TITLE = "round.reportTitle";
    public static final String ROUND_BACK_TO_MAP = "round.backToMap";
    public static final String ROUND_GAME_OVER = "round.gameOver";
    public static final String ROUND_FILTER_LABEL = "round.filter.label";
    public static final String ROUND_FILTER_PRODUCTION = "round.filter.production";
    public static final String ROUND_FILTER_REINFORCEMENT = "round.filter.reinforcement";
    public static final String ROUND_FILTER_BATTLE_WON = "round.filter.battleWon";
    public static final String ROUND_FILTER_BATTLE_LOST = "round.filter.battleLost";
    public static final String ROUND_FILTER_SYSTEM_LOST = "round.filter.systemLost";
    public static final String ROUND_FILTER_DEFENSE_HELD = "round.filter.defenseHeld";

    // Round view — event cards
    public static final String ROUND_EVENT_PRODUCTION = "round.event.production";
    public static final String ROUND_EVENT_REINFORCEMENT = "round.event.reinforcement";
    public static final String ROUND_EVENT_BATTLE_WON_NEUTRAL = "round.event.battleWon.neutral";
    public static final String ROUND_EVENT_BATTLE_WON_ENEMY = "round.event.battleWon.enemy";
    public static final String ROUND_EVENT_BATTLE_LOST = "round.event.battleLost";
    public static final String ROUND_EVENT_SYSTEM_LOST = "round.event.systemLost";
    public static final String ROUND_EVENT_DEFENSE_HELD = "round.event.defenseHeld";
    public static final String ROUND_EVENT_VICTORY = "round.event.victory";

    // Language switcher
    public static final String LANG_LABEL = "lang.label";
    public static final String LANG_DE = "lang.de";
    public static final String LANG_EN = "lang.en";

    // Lobby — host / owner
    public static final String LOBBY_HOST_BADGE = "lobby.host.badge";
    public static final String LOBBY_ABORT_DENIED = "lobby.abort.denied";

    // Presence panel
    public static final String PRESENCE_TITLE = "presence.title";
    public static final String PRESENCE_EMPTY = "presence.empty";
    public static final String PRESENCE_ACTION_FRIEND_REQUEST = "presence.action.friendRequest";
    public static final String PRESENCE_ACTION_CANCEL_REQUEST = "presence.action.cancelRequest";
    public static final String PRESENCE_ACTION_REMOVE_FRIEND = "presence.action.removeFriend";
    public static final String PRESENCE_ACTION_BLOCK = "presence.action.block";
    public static final String PRESENCE_ACTION_UNBLOCK = "presence.action.unblock";
    public static final String PRESENCE_STATUS_FRIEND = "presence.status.friend";
    public static final String PRESENCE_STATUS_PENDING = "presence.status.pending";
    public static final String PRESENCE_STATUS_BLOCKED = "presence.status.blocked";
    public static final String PRESENCE_REQUEST_SENT = "presence.requestSent";
    public static final String PRESENCE_REQUEST_FAILED = "presence.requestFailed";
    public static final String PRESENCE_ACTION_FAILED = "presence.actionFailed";

    // Friend requests inbox
    public static final String FRIEND_INBOX_TITLE = "friend.inbox.title";
    public static final String FRIEND_INBOX_EMPTY = "friend.inbox.empty";
    public static final String FRIEND_INBOX_ACCEPT = "friend.inbox.accept";
    public static final String FRIEND_INBOX_DECLINE = "friend.inbox.decline";

    // Visibility toggle
    public static final String VISIBILITY_LABEL = "visibility.label";
    public static final String VISIBILITY_ALL = "visibility.all";
    public static final String VISIBILITY_FRIENDS_ONLY = "visibility.friendsOnly";
    public static final String VISIBILITY_NONE = "visibility.none";

    // Manage-game dialog + invitations (Phase 4)
    public static final String LOBBY_ACTION_MANAGE = "lobby.action.manage";
    public static final String MANAGE_TITLE = "manage.title";
    public static final String MANAGE_PLAYERS_HEADER = "manage.players.header";
    public static final String MANAGE_PLAYERS_EMPTY = "manage.players.empty";
    public static final String MANAGE_PLAYERS_KICK = "manage.players.kick";
    public static final String MANAGE_PLAYERS_KICK_FAILED = "manage.players.kickFailed";
    public static final String MANAGE_INVITES_HEADER = "manage.invites.header";
    public static final String MANAGE_INVITES_EMPTY = "manage.invites.empty";
    public static final String MANAGE_INVITES_REVOKE = "manage.invites.revoke";
    public static final String MANAGE_INVITE_DROPDOWN = "manage.invite.dropdown";
    public static final String MANAGE_INVITE_PLACEHOLDER = "manage.invite.placeholder";
    public static final String MANAGE_INVITE_SEND = "manage.invite.send";
    public static final String MANAGE_INVITE_FAILED = "manage.invite.failed";
    public static final String MANAGE_INVITE_SUCCESS = "manage.invite.success";
    public static final String MANAGE_CLOSE = "manage.close";
    public static final String INVITATIONS_TITLE = "invitations.title";
    public static final String INVITATIONS_EMPTY = "invitations.empty";
    public static final String INVITATIONS_ROW = "invitations.row";
    public static final String INVITATIONS_ACCEPT = "invitations.accept";
    public static final String INVITATIONS_DECLINE = "invitations.decline";
    public static final String INVITATIONS_ACCEPT_FAILED = "invitations.acceptFailed";
    public static final String INVITATIONS_ACCEPTED = "invitations.accepted";

    // Chat drawer / direct messages
    public static final String CHAT_TITLE = "chat.title";
    public static final String CHAT_ACTION_MESSAGE = "chat.action.message";
    public static final String CHAT_EMPTY_LIST = "chat.empty.list";
    public static final String CHAT_EMPTY_CONVERSATION = "chat.empty.conversation";
    public static final String CHAT_INPUT_PLACEHOLDER = "chat.input.placeholder";
    public static final String CHAT_SEND = "chat.send";
    public static final String CHAT_SEND_EMPTY = "chat.send.empty";
    public static final String CHAT_SEND_OFFLINE = "chat.send.offline";
    public static final String CHAT_SEND_NOT_VISIBLE = "chat.send.notVisible";
    public static final String CHAT_SEND_REJECTED = "chat.send.rejected";
    public static final String CHAT_UNREAD_BADGE = "chat.unread.badge";

    private UiTexts() {}
}
