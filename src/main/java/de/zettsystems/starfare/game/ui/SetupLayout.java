package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Building blocks of the new-game form: sections with a round emblem and choices shown as picture cards.
 * The drawings live in images/wizard; styles/starfare-wizard.css maps each class to its file.
 */
final class SetupLayout {

    /** Round section badges. */
    enum Emblem { GAME, PLAYERS, MAP, CHANCE }

    private SetupLayout() {
    }

    static Div section(Emblem emblem, String title, @Nullable String hint, Component... content) {
        Span badge = new Span();
        badge.addClassNames("setup-emblem", "setup-emblem-" + emblem.name().toLowerCase(Locale.ROOT));
        badge.getElement().setAttribute("aria-hidden", "true");
        H3 heading = new H3(title);
        heading.addClassName("setup-title");
        Div titles = new Div(heading);
        titles.addClassName("setup-titles");
        if (hint != null) {
            Span sub = new Span(hint);
            sub.addClassName("setup-hint");
            titles.add(sub);
        }
        Div head = new Div(badge, titles);
        head.addClassName("setup-head");
        Div section = new Div(head);
        section.add(content);
        section.addClassName("setup-section");
        return section;
    }

    /**
     * Radio group whose items look like cards. The item label stays the accessible name, so tests and screen
     * readers select by the same text the card shows as title.
     */
    static <T> RadioButtonGroup<T> cards(String id, String label, List<T> values, T initial, Card<T> card) {
        RadioButtonGroup<T> group = new RadioButtonGroup<>();
        // The section heading already names most choices; the group keeps its label as accessible name only.
        group.getElement().setAttribute("aria-label", label);
        group.setId(id);
        group.addClassName("setup-cards");
        group.setItems(values);
        group.setItemLabelGenerator(card.title()::apply);
        group.setRenderer(new ComponentRenderer<>(value -> card(card, value)));
        group.setValue(initial);
        return group;
    }

    /** Text and picture of one card; {@code status} adds a small badge when it is not empty. */
    record Card<T>(Function<T, String> title, Function<T, String> description, Function<T, String> picture,
                   Function<T, String> status) {
        Card(Function<T, String> title, Function<T, String> description, Function<T, String> picture) {
            this(title, description, picture, _ -> "");
        }
    }

    /** Fields in one or two columns, depending on the width of the section. */
    static FormLayout grid(Component... fields) {
        FormLayout grid = new FormLayout(fields);
        grid.addClassName("setup-grid");
        grid.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("28em", 2));
        return grid;
    }

    private static <T> Div card(Card<T> card, T value) {
        Span picture = new Span();
        picture.addClassNames("setup-picture", "setup-picture-" + card.picture().apply(value));
        picture.getElement().setAttribute("aria-hidden", "true");
        Span heading = new Span(card.title().apply(value));
        heading.addClassName("setup-card-title");
        Span text = new Span(card.description().apply(value));
        text.addClassName("setup-card-text");
        Div result = new Div(picture, heading);
        String status = card.status().apply(value);
        if (!status.isEmpty()) {
            Span badge = new Span(status);
            badge.addClassName("setup-card-status");
            result.add(badge);
        }
        result.add(text);
        result.addClassName("setup-card");
        return result;
    }
}
