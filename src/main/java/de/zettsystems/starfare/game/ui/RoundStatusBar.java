package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.zettsystems.starfare.game.values.RoundStatus;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.CssProperties;
import de.zettsystems.starfare.style.HtmlAttributes;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Leiste unter dem Kopf: alle menschlichen Mitspieler mit Haken, sobald sie abgegeben haben.
 * Läuft die Nachzügler-Uhr, steht der Countdown beim letzten Fehlenden, sonst am Ende der
 * Leiste als Rundenende. Der Countdown zählt im Browser herunter; der Server beendet die
 * Runde selbst und löst damit ohnehin einen Refresh aus.
 */
final class RoundStatusBar extends HorizontalLayout {

    RoundStatusBar() {
        addClassName("round-status");
        setSpacing(false);
        setPadding(false);
        setAlignItems(Alignment.CENTER);
        setVisible(false);
    }

    void update(RoundStatus status) {
        removeAll();
        setVisible(status.seats().size() >= 2);
        if (!isVisible()) {
            return;
        }
        Instant deadline = status.deadline();
        for (RoundStatus.Seat seat : status.seats()) {
            boolean straggler = Objects.equals(status.stragglerId(), seat.playerId());
            add(seatChip(seat, straggler ? deadline : null));
        }
        if (deadline != null && status.stragglerId() == null) {
            Span label = new Span(I18n.t(UiTexts.ROUND_STATUS_ENDS_IN));
            label.addClassName("round-status-ends-label");
            Span roundEnd = new Span(label, countdown(deadline));
            roundEnd.addClassName("round-status-ends");
            add(roundEnd);
        }
    }

    private static Span seatChip(RoundStatus.Seat seat, @Nullable Instant stragglerDeadline) {
        Span dot = new Span();
        dot.addClassName("round-status-dot");
        dot.getStyle().set(CssProperties.BACKGROUND, seat.colorHex());
        Span name = new Span(seat.label());
        name.addClassName("round-status-name");
        Icon icon = (seat.submitted() ? VaadinIcon.CHECK : VaadinIcon.HOURGLASS).create();
        icon.addClassName("round-status-icon");

        Span chip = new Span(dot, name, icon);
        chip.addClassName("round-status-seat");
        chip.addClassName(seat.submitted() ? "submitted" : "pending");
        String state = I18n.t(seat.submitted() ? UiTexts.ROUND_STATUS_SUBMITTED : UiTexts.ROUND_STATUS_PENDING);
        if (stragglerDeadline != null) {
            chip.addClassName("straggler");
            chip.add(countdown(stragglerDeadline));
            state = I18n.t(UiTexts.ROUND_STATUS_STRAGGLER);
        }
        chip.getElement().setProperty(HtmlAttributes.TITLE, seat.label() + " – " + state);
        return chip;
    }

    /** Übergibt die Restzeit statt des Zeitpunkts, damit eine falsch gehende Browseruhr nicht stört. */
    private static Span countdown(Instant deadline) {
        Span clock = new Span();
        clock.addClassName("round-status-clock");
        long remainingMillis = Math.max(0, Duration.between(Instant.now(), deadline).toMillis());
        clock.getElement().executeJs("""
                const el = this;
                clearInterval(el._sfTimer);
                const end = Date.now() + $0;
                const pad = n => String(n).padStart(2, '0');
                const tick = () => {
                  // Nach einem Refresh ist der alte Chip weg; sein Timer darf nicht weiterlaufen.
                  if (!el.isConnected) { clearInterval(el._sfTimer); return; }
                  const s = Math.max(0, Math.round((end - Date.now()) / 1000));
                  const h = Math.floor(s / 3600), m = Math.floor(s % 3600 / 60), sec = s % 60;
                  el.textContent = h > 0 ? h + ':' + pad(m) + ':' + pad(sec) : m + ':' + pad(sec);
                  if (s === 0) { clearInterval(el._sfTimer); }
                };
                tick();
                el._sfTimer = setInterval(tick, 1000);
                """, (double) remainingMillis);
        return clock;
    }
}
