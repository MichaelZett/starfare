package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.VaadinSession;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameConfig;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.report.values.BattleReplay;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import de.zettsystems.starfare.style.CssProperties;
import jakarta.annotation.security.PermitAll;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Visual round-report screen. Shown after a turn with significant events.
 * Displays structured {@link TurnEvent}s as animated cards.
 */
@Route("round/:gameId")
@CssImport("./styles/starfare.css")
@JsModule("./battle-replay.js")
@PermitAll
public class RoundView extends VerticalLayout implements BeforeEnterObserver {
    private static final String FILTER_SESSION_KEY = "starfare.roundFilter";

    enum EventCategory {PRODUCTION, REINFORCEMENT, BATTLE_WON, BATTLE_LOST, SYSTEM_LOST, DEFENSE_HELD}

    private final GameService game;
    private final H2 reportHeader = new H2();
    private final H2 gameOverHeader = new H2();
    private final Div filterBar = new Div();
    private final Div timeline = new Div();
    private final Button back;
    private @Nullable GameId gameId;
    private List<TurnEvent> lastEvents = List.of();
    private boolean battlePresentationEnabled;
    private @Nullable TurnReport currentReport;
    private boolean finished;

    @Autowired
    public RoundView(GameService game) {
        this.game = game;
        addClassName("round-root");
        setSizeFull();

        H1 header = new H1(I18n.t(UiTexts.ROUND_HEADER_TITLE));
        header.addClassName("app-header");

        gameOverHeader.setVisible(false);
        gameOverHeader.getStyle().set(CssProperties.COLOR, "#6d28d9").set(CssProperties.FONT_WEIGHT, "700");

        filterBar.addClassName("report-filter-bar");
        buildFilterBar();

        timeline.addClassName("report-timeline");

        back = new Button(I18n.t(UiTexts.ROUND_BACK_TO_MAP), _ -> {
            GameId current = gameId;
            if (current == null) {
                getUI().ifPresent(ui -> ui.navigate(LobbyView.class));
                return;
            }
            getUI().ifPresent(ui -> ui.navigate(MainView.class,
                    new RouteParameters("gameId", current.value())));
        });

        add(header, reportHeader, gameOverHeader, filterBar, timeline, back);
    }

    private void buildFilterBar() {
        filterBar.removeAll();
        Span label = new Span(I18n.t(UiTexts.ROUND_FILTER_LABEL));
        label.addClassName("report-filter-label");
        filterBar.add(label);
        EnumSet<EventCategory> enabled = enabledCategories();
        addFilterCheckbox(enabled, EventCategory.PRODUCTION, UiTexts.ROUND_FILTER_PRODUCTION);
        addFilterCheckbox(enabled, EventCategory.REINFORCEMENT, UiTexts.ROUND_FILTER_REINFORCEMENT);
        addFilterCheckbox(enabled, EventCategory.BATTLE_WON, UiTexts.ROUND_FILTER_BATTLE_WON);
        addFilterCheckbox(enabled, EventCategory.BATTLE_LOST, UiTexts.ROUND_FILTER_BATTLE_LOST);
        addFilterCheckbox(enabled, EventCategory.SYSTEM_LOST, UiTexts.ROUND_FILTER_SYSTEM_LOST);
        addFilterCheckbox(enabled, EventCategory.DEFENSE_HELD, UiTexts.ROUND_FILTER_DEFENSE_HELD);
        Checkbox presentation = new Checkbox(I18n.t(UiTexts.BATTLE_PRESENTATION_TOGGLE), battlePresentationEnabled);
        presentation.addValueChangeListener(event -> {
            GameId current = gameId;
            if (current != null) {
                battlePresentationEnabled = event.getValue();
                BattlePresentationPreference.set(current, reportTurn(), battlePresentationEnabled);
                renderTimeline();
            }
        });
        filterBar.add(presentation);
    }

    private void addFilterCheckbox(EnumSet<EventCategory> enabled, EventCategory cat, String textKey) {
        Checkbox cb = new Checkbox(I18n.t(textKey), enabled.contains(cat));
        cb.addClassName("report-filter-item");
        cb.addValueChangeListener(e -> {
            EnumSet<EventCategory> current = enabledCategories();
            if (Boolean.TRUE.equals(e.getValue())) {
                current.add(cat);
            } else {
                current.remove(cat);
            }
            VaadinSession.getCurrent().setAttribute(FILTER_SESSION_KEY, current);
            renderTimeline();
        });
        filterBar.add(cb);
    }

    @SuppressWarnings("unchecked")
    private EnumSet<EventCategory> enabledCategories() {
        Object raw = VaadinSession.getCurrent().getAttribute(FILTER_SESSION_KEY);
        if (raw instanceof EnumSet<?> set) {
            return EnumSet.copyOf((EnumSet<EventCategory>) set);
        }
        return EnumSet.allOf(EventCategory.class);
    }

    private static Optional<EventCategory> categoryOf(TurnEvent e) {
        return Optional.ofNullable(switch (e) {
            case TurnEvent.Production _ -> EventCategory.PRODUCTION;
            case TurnEvent.Reinforcement _ -> EventCategory.REINFORCEMENT;
            case TurnEvent.BattleWon _ -> EventCategory.BATTLE_WON;
            case TurnEvent.BattleLost _ -> EventCategory.BATTLE_LOST;
            case TurnEvent.SystemLost _ -> EventCategory.SYSTEM_LOST;
            case TurnEvent.DefenseHeld _ -> EventCategory.DEFENSE_HELD;
            case TurnEvent.Victory _, TurnEvent.Defeat _ -> null;
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String parameter = event.getRouteParameters().get("gameId").orElse(null);
        if (parameter == null || parameter.isBlank()) {
            event.forwardTo(LobbyView.class);
            return;
        }
        GameId candidate = GameId.of(parameter);
        if (game.viewForAccount(candidate, UserContext.currentPlayerId().orElse("")).isEmpty()) {
            event.forwardTo(LobbyView.class);
            return;
        }
        this.gameId = candidate;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (gameId == null) {
            getUI().ifPresent(ui -> ui.navigate(LobbyView.class));
            return;
        }
        String playerId = UserContext.currentPlayerId().orElse(null);
        int seat = playerId == null ? -1 : game.seatFor(gameId, playerId).orElse(-1);
        if (seat < 0) {
            getUI().ifPresent(ui -> ui.navigate(LobbyView.class));
            return;
        }
        PlayerViewState view = game.viewForAccount(gameId, UserContext.currentPlayerId().orElse("")).orElse(null);
        if (view == null) { getUI().ifPresent(ui -> ui.navigate(LobbyView.class)); return; }
        reportHeader.setText(I18n.t(UiTexts.ROUND_REPORT_TITLE, view.turn() - 1));
        finished = view.gameOver();
        if (finished) {
            gameOverHeader.setText(I18n.t(UiTexts.ROUND_GAME_OVER, winnerName(view)));
            gameOverHeader.setVisible(true);
        }

        var report = view.report();
        currentReport = report;
        lastEvents = report != null ? report.events() : List.of();
        battlePresentationEnabled = BattlePresentationPreference.enabled(gameId, view.turn() - 1,
                view.battlePresentationEnabled());
        buildFilterBar();
        renderTimeline();
    }

    private void renderTimeline() {
        timeline.removeAll();
        boolean pending = hasPendingBattles();
        gameOverHeader.setVisible(finished && !pending);
        GameId current = gameId;
        if (current != null && game.viewForAccount(current, UserContext.currentPlayerId().orElse("")).isEmpty()) {
            lastEvents = List.of();
            getUI().ifPresent(ui -> ui.navigate(LobbyView.class));
            return;
        }
        if (lastEvents.isEmpty()) {
            timeline.add(new Paragraph(I18n.t(UiTexts.ROUND_NO_EVENTS)));
            return;
        }
        EnumSet<EventCategory> enabled = enabledCategories();
        List<TurnEvent> filtered = lastEvents.stream()
                .filter(ev -> !pending || !(ev instanceof TurnEvent.Victory || ev instanceof TurnEvent.Defeat))
                .filter(ev -> categoryOf(ev).map(enabled::contains).orElse(true))
                .toList();
        if (filtered.isEmpty()) {
            timeline.add(new Paragraph(I18n.t(UiTexts.ROUND_NO_EVENTS_FILTERED)));
            return;
        }
        for (int i = 0; i < filtered.size(); i++) {
            timeline.add(buildCard(filtered.get(i), i));
        }
    }

    private Div buildCard(TurnEvent event, int index) {
        Div card = new Div();
        card.addClassName("event-card");
        card.getStyle().set(CssProperties.ANIMATION_DELAY, (index * 0.1) + "s");

        boolean pending = isPending(event);
        String icon = pending ? "⚔" : iconFor(event);
        String cssClass = pending ? "event-battle-pending" : cssFor(event);
        String text = textFor(event, pending);

        card.addClassName(cssClass);
        var iconSpan = new Span(icon);
        iconSpan.addClassName("event-icon");
        var textSpan = new Span(text);
        card.add(iconSpan, textSpan);
        BattleReplay replay = BattleReplay.from(event).orElse(null);
        if (battlePresentationEnabled && replay != null) {
            card.addClassName("event-battle-interactive");
            card.addClickListener(_ -> BattleReplayDialog.open(replay, battleSides(event), () -> acknowledge(event)));
        } else if (pending) {
            card.addClassName("event-battle-interactive");
            card.addClickListener(_ -> acknowledge(event));
        }
        return card;
    }

    private static BattleReplayDialog.BattleSides battleSides(TurnEvent event) {
        return switch (event) {
            case TurnEvent.BattleWon battle -> new BattleReplayDialog.BattleSides(I18n.t(UiTexts.BATTLE_REPLAY_YOU),
                    battle.wasNeutral() ? I18n.t(UiTexts.BATTLE_REPLAY_NEUTRAL) : I18n.t(UiTexts.BATTLE_REPLAY_OPPONENT));
            case TurnEvent.BattleLost _ -> new BattleReplayDialog.BattleSides(I18n.t(UiTexts.BATTLE_REPLAY_YOU),
                    I18n.t(UiTexts.BATTLE_REPLAY_OPPONENT));
            case TurnEvent.SystemLost _, TurnEvent.DefenseHeld _ -> new BattleReplayDialog.BattleSides(
                    I18n.t(UiTexts.BATTLE_REPLAY_OPPONENT), I18n.t(UiTexts.BATTLE_REPLAY_YOU));
            case TurnEvent.Production _, TurnEvent.Reinforcement _, TurnEvent.Victory _, TurnEvent.Defeat _ ->
                    new BattleReplayDialog.BattleSides("", "");
        };
    }

    private boolean hasPendingBattles() {
        return gameId != null && currentReport != null
                && !BattleAcknowledgements.pending(gameId, currentReport).isEmpty();
    }

    private boolean isPending(TurnEvent event) {
        return gameId != null && currentReport != null && event.battleSystemId().isPresent()
                && BattleAcknowledgements.pending(gameId, currentReport).contains(event.battleSystemId().getAsInt());
    }

    private void acknowledge(TurnEvent event) {
        if (gameId != null && currentReport != null) {
            BattleAcknowledgements.acknowledge(gameId, currentReport, event);
            renderTimeline();
        }
    }

    private static String iconFor(TurnEvent e) {
        return switch (e) {
            case TurnEvent.Production _ -> "⬆";
            case TurnEvent.Reinforcement _ -> "→";
            case TurnEvent.BattleWon _ -> "⚔";
            case TurnEvent.BattleLost _ -> "✕";
            case TurnEvent.SystemLost _ -> "☠";
            case TurnEvent.DefenseHeld _ -> "🛡";
            case TurnEvent.Victory _ -> "🏆";
            case TurnEvent.Defeat _ -> "☠";
        };
    }

    private static String cssFor(TurnEvent e) {
        return switch (e) {
            case TurnEvent.Production _ -> "event-production";
            case TurnEvent.Reinforcement _ -> "event-reinforcement";
            case TurnEvent.BattleWon _ -> "event-battle-won";
            case TurnEvent.BattleLost _ -> "event-battle-lost";
            case TurnEvent.SystemLost _ -> "event-system-lost";
            case TurnEvent.DefenseHeld _ -> "event-defense-held";
            case TurnEvent.Victory _ -> "event-victory";
            case TurnEvent.Defeat _ -> "event-system-lost";
        };
    }

    private String textFor(TurnEvent e, boolean presentationEnabled) {
        return switch (e) {
            case TurnEvent.Production p -> I18n.t(UiTexts.ROUND_EVENT_PRODUCTION, p.systemName(), p.amount());
            case TurnEvent.Reinforcement r -> I18n.t(UiTexts.ROUND_EVENT_REINFORCEMENT,
                    r.systemName(), r.ships(), r.totalGarrison(), r.fleetLabel());
            case TurnEvent.BattleWon b -> presentationEnabled
                    ? I18n.t(UiTexts.ROUND_EVENT_BATTLE_READY, b.systemName())
                    : I18n.t(b.wasNeutral() ? UiTexts.ROUND_EVENT_BATTLE_WON_NEUTRAL
                    : UiTexts.ROUND_EVENT_BATTLE_WON_ENEMY, b.systemName(), b.attacking(), b.defending(), b.remaining());
            case TurnEvent.BattleLost b -> presentationEnabled
                    ? I18n.t(UiTexts.ROUND_EVENT_BATTLE_READY, b.systemName())
                    : I18n.t(UiTexts.ROUND_EVENT_BATTLE_LOST,
                    b.systemName(), b.attacking(), b.defending(), b.defendersLeft());
            case TurnEvent.SystemLost l -> presentationEnabled
                    ? I18n.t(UiTexts.ROUND_EVENT_BATTLE_READY, l.systemName())
                    : I18n.t(UiTexts.ROUND_EVENT_SYSTEM_LOST, l.systemName());
            case TurnEvent.DefenseHeld d -> presentationEnabled
                    ? I18n.t(UiTexts.ROUND_EVENT_BATTLE_READY, d.systemName())
                    : I18n.t(UiTexts.ROUND_EVENT_DEFENSE_HELD,
                            d.systemName(), d.attacking(), d.defendersLeft());
            case TurnEvent.Victory _ -> I18n.t(UiTexts.ROUND_EVENT_VICTORY,
                    GameConfig.VICTORY_SYSTEM_PERCENT);
            case TurnEvent.Defeat defeat -> I18n.t(UiTexts.ROUND_EVENT_DEFEAT, defeat.winnerName());
        };
    }

    private int reportTurn() {
        PlayerViewState view = gameId == null ? null
                : game.viewForAccount(gameId, UserContext.currentPlayerId().orElse("")).orElse(null);
        return view == null ? 0 : view.turn() - 1;
    }

    private String winnerName(PlayerViewState view) {
        Integer winnerId = view.winnerId();
        if (winnerId == null) {
            return "?";
        }
        return view.players().stream()
                .filter(p -> p.id() == winnerId)
                .map(Player::label)
                .findFirst().orElse("?");
    }
}
