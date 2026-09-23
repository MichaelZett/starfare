package de.zettsystems.starfare.game.ui;

/**
 * Central registry of i18n keys. Resolve via Component#getTranslation or {@link de.zettsystems.starfare.i18n.I18n}.
 */
public final class UiTexts {
    public static final String MAP_COLUMN_ARRIVAL_TURN = "map.column.arrivalTurn";
    public static final String MAP_PRODUCTION_INCOMING = "map.production.incoming";
    public static final String MAP_PRODUCTION_OUTGOING = "map.production.outgoing";
    public static final String MAP_PRODUCTION_FLOW = "map.production.flow";
    public static final String MAP_SIDEBAR_CONTACTS = "map.sidebar.contacts";
    public static final String MAP_SIDEBAR_DETAILS = "map.sidebar.details";
    public static final String MAP_SIDEBAR_FLEETS = "map.sidebar.fleets";
    public static final String MAP_SIDEBAR_ORDERS = "map.sidebar.orders";
    public static final String MAP_SIDEBAR_RELOCATIONS = "map.sidebar.relocations";
    public static final String MAP_SIDEBAR_LOGISTICS = "map.sidebar.logistics";
    public static final String MAP_LOGISTICS_INTRO = "map.logistics.intro";
    public static final String MAP_LOGISTICS_EMPTY = "map.logistics.empty";
    public static final String MAP_LOGISTICS_ROUTES = "map.logistics.routes";
    public static final String MAP_LOGISTICS_SOURCE = "map.logistics.source";
    public static final String MAP_LOGISTICS_SINK = "map.logistics.sink";
    public static final String MAP_LOGISTICS_BALANCED = "map.logistics.balanced";
    public static final String MAP_LOGISTICS_RESERVE_AVAILABLE = "map.logistics.reserveAvailable";
    public static final String MAP_LOGISTICS_PER_TURN = "map.logistics.perTurn";
    public static final String MAP_SIDEBAR_REPORT = "map.sidebar.report";
    public static final String MAP_SIDEBAR_OPEN_REPORT = "map.sidebar.openReport";
    public static final String MAP_SIDEBAR_REPORT_HINT = "map.sidebar.reportHint";
    public static final String MAP_SIDEBAR_DETAILS_EMPTY = "map.sidebar.detailsEmpty";
    public static final String MAP_SIDEBAR_SYSTEMS = "map.sidebar.systems";
    public static final String MAP_SIDEBAR_PRODUCTION = "map.sidebar.production";
    public static final String MAP_SIDEBAR_SHIPS = "map.sidebar.ships";
    public static final String MAP_SIDEBAR_FLEET_COUNT = "map.sidebar.fleetCount";
    public static final String MAP_SIDEBAR_SYSTEM = "map.sidebar.system";
    public static final String MAP_SIDEBAR_FLEET = "map.sidebar.fleet";
    public static final String MAP_SIDEBAR_GARRISON = "map.sidebar.garrison";
    public static final String MAP_CAPTURE_SUMMARY_TITLE = "map.captureSummary.title";
    public static final String MAP_CAPTURE_SUMMARY_CLOSE = "map.captureSummary.close";
    public static final String MAP_SIDEBAR_CONTACTS_EMPTY = "map.sidebar.contactsEmpty";
    public static final String MAP_SIDEBAR_OWNER = "map.sidebar.owner";
    public static final String MAP_SIDEBAR_ESTIMATED_SHIPS = "map.sidebar.estimatedShips";
    public static final String MAP_SIDEBAR_COUNTED_SHIPS = "map.sidebar.countedShips";
    public static final String MAP_SIDEBAR_LAST_CONTACT = "map.sidebar.lastContact";
    public static final String MAP_SIDEBAR_INTELLIGENCE = "map.sidebar.intelligence";
    public static final String MAP_SIDEBAR_LAST_SEEN_TURN = "map.sidebar.lastSeenTurn";
    public static final String MAP_SIDEBAR_OWNED_SINCE = "map.sidebar.ownedSince";
    public static final String MAP_SIDEBAR_PREVIOUS_OWNER = "map.sidebar.previousOwner";
    public static final String MAP_SIDEBAR_UNTIL_TURN = "map.sidebar.untilTurn";
    public static final String MAP_SIDEBAR_NONE = "map.sidebar.none";
    public static final String MAP_SIDEBAR_FLEET_LAUNCHED = "map.sidebar.fleetLaunched";
    public static final String MAP_SIDEBAR_FLEET_TRAVELLING = "map.sidebar.fleetTravelling";
    public static final String REVIEW_PERSPECTIVE = "review.perspective";
    public static final String REVIEW_FOG = "review.fog";
    public static final String SPECTATOR_PERSPECTIVE = "spectator.perspective";
    public static final String REPLAY_TIMELINE_TURN = "replay.timeline.turn";
    public static final String REPLAY_TIMELINE_FINAL = "replay.timeline.final";
    // Lobby — header / toolbar / empty state
    public static final String LOBBY_HEADER_TITLE = "lobby.header.title";
    public static final String LOBBY_SUBTITLE = "lobby.subtitle";
    public static final String LOBBY_EMPTY_TITLE = "lobby.empty.title";
    public static final String LOBBY_EMPTY_BODY = "lobby.empty.body";
    public static final String LOBBY_NEW_GAME = "lobby.newGame";
    public static final String LOBBY_JOINED = "lobby.joined";
    public static final String LOBBY_JOIN_FAILED = "lobby.joinFailed";
    public static final String LOBBY_JOIN_TITLE = "lobby.join.title";
    public static final String LOBBY_EMPIRE_NAME_REQUIRED = "lobby.empireName.required";
    public static final String LOBBY_START_FAILED = "lobby.startFailed";

    // Lobby — grid
    public static final String LOBBY_COLUMN_GAME = "lobby.column.game";
    public static final String LOBBY_COLUMN_TURN = "lobby.column.turn";
    public static final String LOBBY_COLUMN_PLAYERS = "lobby.column.players";
    public static final String LOBBY_COLUMN_STATUS = "lobby.column.status";
    public static final String LOBBY_COLUMN_ACTIONS = "lobby.column.actions";
    public static final String LOBBY_TURN_LABEL = "lobby.turnLabel";
    public static final String LOBBY_SEARCH = "lobby.search";
    public static final String LOBBY_FILTER = "lobby.filter";
    public static final String LOBBY_FILTER_ALL = "lobby.filter.all";
    public static final String LOBBY_FILTER_OWN = "lobby.filter.own";
    public static final String LOBBY_FILTER_OPEN = "lobby.filter.open";
    public static final String LOBBY_FILTER_RUNNING = "lobby.filter.running";
    public static final String LOBBY_FILTER_FINISHED = "lobby.filter.finished";

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
    public static final String LOBBY_WIZARD_JOIN_AFTER_CREATE = "lobby.wizard.joinAfterCreate";
    public static final String LOBBY_FIELD_SYSTEMS = "lobby.field.systems";
    public static final String LOBBY_FIELD_HUMANS = "lobby.field.humans";
    public static final String LOBBY_FIELD_AI = "lobby.field.ai";
    public static final String LOBBY_FIELD_COLOR = "lobby.field.color";
    public static final String LOBBY_FIELD_EMPIRE_NAME = "lobby.field.empireName";
    public static final String LOBBY_FIELD_PRODUCTION_DISTRIBUTION = "lobby.field.productionDistribution";
    public static final String LOBBY_PRODUCTION_DISTRIBUTION_UNIFORM = "lobby.productionDistribution.uniform";
    public static final String LOBBY_PRODUCTION_DISTRIBUTION_GAUSSIAN = "lobby.productionDistribution.gaussian";
    public static final String LOBBY_FIELD_GALAXY_LAYOUT = "lobby.field.galaxyLayout";
    public static final String LOBBY_GALAXY_LAYOUT_RANDOM = "lobby.galaxyLayout.random";
    public static final String LOBBY_GALAXY_LAYOUT_EVEN = "lobby.galaxyLayout.even";
    public static final String LOBBY_WIZARD_SECTION_ROUNDS = "lobby.wizard.section.rounds";
    public static final String LOBBY_WIZARD_SECTION_ROUNDS_HINT = "lobby.wizard.section.rounds.hint";
    public static final String LOBBY_WIZARD_SECTION_COMBAT = "lobby.wizard.section.combat";
    public static final String LOBBY_WIZARD_SECTION_COMBAT_HINT = "lobby.wizard.section.combat.hint";
    public static final String LOBBY_FIELD_ROUND_LIMIT = "lobby.field.roundLimit";
    public static final String LOBBY_FIELD_STRAGGLER_LIMIT = "lobby.field.stragglerLimit";
    public static final String LOBBY_FIELD_ATTACK_ORDER = "lobby.field.attackOrder";
    public static final String LOBBY_ATTACK_ORDER_RANDOM = "lobby.attackOrder.random";
    public static final String LOBBY_ATTACK_ORDER_STRONGEST_FIRST = "lobby.attackOrder.strongestFirst";
    public static final String DURATION_SECONDS = "duration.seconds";
    public static final String DURATION_MINUTES = "duration.minutes";
    public static final String DURATION_HOURS = "duration.hours";
    public static final String DURATION_DAYS = "duration.days";
    public static final String ROUND_STATUS_ENDS_IN = "roundStatus.endsIn";
    public static final String ROUND_STATUS_SUBMITTED = "roundStatus.submitted";
    public static final String ROUND_STATUS_PENDING = "roundStatus.pending";
    public static final String ROUND_STATUS_STRAGGLER = "roundStatus.straggler";
    public static final String LOBBY_FIELD_NEUTRAL_MIN_PRODUCTION = "lobby.field.neutralMinProduction";
    public static final String LOBBY_FIELD_NEUTRAL_MAX_PRODUCTION = "lobby.field.neutralMaxProduction";
    public static final String LOBBY_FIELD_START_PRODUCTION_HUMAN = "lobby.field.startProduction.human";
    public static final String LOBBY_FIELD_START_PRODUCTION_AI = "lobby.field.startProduction.ai";
    public static final String LOBBY_FIELD_START_GARRISON = "lobby.field.startGarrison";
    public static final String LOBBY_FIELD_OBSERVERS_ALLOWED = "lobby.field.observersAllowed";
    public static final String LOBBY_FIELD_REENTRY_ALLOWED = "lobby.field.reentryAllowed";
    public static final String LOBBY_FIELD_BATTLE_PRESENTATION = "lobby.field.battlePresentation";
    public static final String LOBBY_FIELD_COMBAT_RANDOMNESS = "lobby.field.combatRandomness";
    public static final String LOBBY_FIELD_COMBAT_RANDOMNESS_VALUE = "lobby.field.combatRandomness.value";
    public static final String LOBBY_FIELD_COMBAT_RANDOMNESS_HINT = "lobby.field.combatRandomness.hint";

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
    public static final String MAP_SEND_QUICK_EXCEPT_PRODUCTION = "map.send.quick.exceptProduction";
    public static final String MAP_STANDING_ORDER_CHECKBOX = "map.standingOrder.checkbox";
    public static final String MAP_INVALID_COMMAND = "map.invalidCommand";
    public static final String MAP_DIALOG_CANCEL = "map.dialog.cancel";
    public static final String MAP_ADD_STANDING_ORDER_FAILED = "map.addStandingOrder.failed";
    public static final String MAP_SEND_FLEET_PREVIEW = "map.send.fleetPreview";
    public static final String MAP_SEND_RELOCATION_PREVIEW = "map.send.relocationPreview";

    // Map — fleets grid
    public static final String MAP_OWN_FLEETS = "map.ownFleets";
    public static final String MAP_COLUMN_NO = "map.column.no";
    public static final String MAP_COLUMN_FROM = "map.column.from";
    public static final String MAP_COLUMN_TO = "map.column.to";
    public static final String MAP_COLUMN_SHIPS = "map.column.ships";
    public static final String MAP_COLUMN_ETA = "map.column.eta";
    public static final String MAP_FLEET_TRAVEL_PROGRESS = "map.fleet.travelProgress";
    public static final String ROUND_FILTER_SUMMARY = "round.filter.summary";
    public static final String LOBBY_CARD_MORE_ACTIONS = "lobby.card.moreActions";
    public static final String LOBBY_WIZARD_ADVANCED = "lobby.wizard.advanced";
    public static final String LOBBY_WIZARD_SECTION_SETTINGS = "lobby.wizard.section.settings";
    public static final String LOBBY_WIZARD_SECTION_SETTINGS_HINT = "lobby.wizard.section.settings.hint";
    public static final String STATISTICS_BALANCE = "statistics.balance";
    public static final String REPLAY_TIMELINE_PREVIOUS = "replay.timeline.previous";
    public static final String REPLAY_TIMELINE_NEXT = "replay.timeline.next";
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
    public static final String MAP_COLUMN_STANDING_SHIPS = "map.column.standing.ships";
    public static final String MAP_ACTION_DELETE_STANDING = "map.action.deleteStanding";
    public static final String MAP_ACTION_EDIT_STANDING = "map.action.editStanding";
    public static final String MAP_REMOVE_STANDING_ORDER_FAILED = "map.removeStandingOrder.failed";
    public static final String MAP_SHOW_STANDING_TOGGLE = "map.showStanding.toggle";
    public static final String MAP_ORDER_TYPE_STANDING = "map.orderType.standing";
    public static final String MAP_HINT_PICK_SOURCE = "map.hint.pickSource";
    public static final String MAP_HINT_INSPECT_SYSTEM = "map.hint.inspectSystem";
    public static final String MAP_HINT_PICK_TARGET = "map.hint.pickTarget";
    public static final String MAP_HINT_FLEET_LANE = "map.hint.fleetLane";
    public static final String MAP_PLANNED_LANE_TOOLTIP = "map.plannedLane.tooltip";
    public static final String MAP_STANDING_LANE_TOOLTIP = "map.standingLane.tooltip";
    public static final String MAP_HINT_DRAG_RELOCATION = "map.hint.dragRelocation";
    public static final String MAP_REPORT_MARKER_HINT = "map.reportMarker.hint";

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
    public static final String ROUND_EVENT_DEFEAT = "round.event.defeat";
    public static final String ROUND_EVENT_OPEN_MAP = "round.event.openMap";
    public static final String ROUND_EVENT_BATTLE_READY = "round.event.battleReady";
    public static final String BATTLE_REPLAY_TITLE = "battleReplay.title";
    public static final String BATTLE_REPLAY_ATTACKERS = "battleReplay.attackers";
    public static final String BATTLE_REPLAY_DEFENDERS = "battleReplay.defenders";
    public static final String BATTLE_REPLAY_YOU = "battleReplay.you";
    public static final String BATTLE_REPLAY_OPPONENT = "battleReplay.opponent";
    public static final String BATTLE_REPLAY_NEUTRAL = "battleReplay.neutral";
    public static final String BATTLE_REPLAY_STRENGTH = "battleReplay.strength";
    public static final String BATTLE_REPLAY_RESULT = "battleReplay.result";
    public static final String BATTLE_REPLAY_SOUND = "battleReplay.sound";
    public static final String BATTLE_REPLAY_SOUND_TEST = "battleReplay.soundTest";
    public static final String BATTLE_REPLAY_CLOSE = "battleReplay.close";
    public static final String BATTLE_REPLAY_ACKNOWLEDGE = "battleReplay.acknowledge";
    public static final String BATTLE_PRESENTATION_TOGGLE = "battlePresentation.toggle";
    public static final String GAME_OUTCOME_VICTORY = "gameOutcome.victory";
    public static final String GAME_OUTCOME_DEFEAT = "gameOutcome.defeat";
    public static final String GAME_OUTCOME_ROUNDS = "gameOutcome.rounds";
    public static final String GAME_OUTCOME_SYSTEMS = "gameOutcome.systems";
    public static final String GAME_OUTCOME_BUILT = "gameOutcome.built";
    public static final String GAME_OUTCOME_DESTROYED = "gameOutcome.destroyed";
    public static final String GAME_OUTCOME_LOST = "gameOutcome.lost";
    public static final String GAME_OUTCOME_CONTINUE = "gameOutcome.continue";

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
    public static final String GAME_PRIVATE = "game.visibility.private";
    public static final String GAME_PUBLIC = "game.visibility.public";
    public static final String GAME_PRIVATE_HINT = "game.visibility.hint";
    public static final String ARCHIVE_TITLE = "archive.title";
    public static final String ARCHIVE_VIEW = "archive.view";
    public static final String ARCHIVE_WINNER = "archive.winner";
    public static final String ARCHIVE_NO_WINNER = "archive.noWinner";
    public static final String ARCHIVE_FINISHED = "archive.finished";
    public static final String ARCHIVE_UNKNOWN_TIME = "archive.unknownTime";
    public static final String ARCHIVE_EMPTY = "archive.empty";
    public static final String MAP_SIDEBAR_GARRISON_RESERVE = "map.sidebar.garrisonReserve";
    public static final String MAP_SIDEBAR_GARRISON_RESERVE_SAVE = "map.sidebar.garrisonReserve.save";
    public static final String STATISTICS_TITLE = "statistics.title";
    public static final String STATISTICS_TOTALS = "statistics.totals";
    public static final String STATISTICS_OPPONENT = "statistics.opponent";
    public static final String STATISTICS_GAMES = "statistics.games";
    public static final String STATISTICS_WINS = "statistics.wins";
    public static final String STATISTICS_LOSSES = "statistics.losses";
    public static final String STATISTICS_EMPTY = "statistics.empty";
    public static final String STATISTICS_HISTORY = "statistics.history";
    public static final String STATISTICS_GAME = "statistics.game";
    public static final String STATISTICS_RESULT = "statistics.result";
    public static final String STATISTICS_FINISHED = "statistics.finished";
    public static final String STATISTICS_NO_OPPONENTS = "statistics.noOpponents";
    public static final String STATISTICS_WIN = "statistics.win";
    public static final String STATISTICS_LOSS = "statistics.loss";
    public static final String STATISTICS_DRAW = "statistics.draw";
}
