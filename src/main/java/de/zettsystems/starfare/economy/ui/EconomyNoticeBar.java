package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import de.zettsystems.starfare.economy.values.EconomyIssues;
import de.zettsystems.starfare.i18n.I18n;
import java.util.List;
import java.util.function.Consumer;

/** Compact problem groups; each opens the filtered logistics list. */
public final class EconomyNoticeBar extends Div {
    public EconomyNoticeBar() { addClassName("economy-notices"); }
    public void show(List<EconomyIssues.Issue> issues, Consumer<EconomyIssues.Kind> open) {
        removeAll();
        for (var kind : EconomyIssues.Kind.values()) {
            long count = issues.stream().filter(issue -> issue.reasons().contains(kind)).count();
            if (count == 0) { continue; }
            Button button = new Button(I18n.t(key(kind), count), _ -> open.accept(kind));
            button.setId("economy-notice-" + kind.name().toLowerCase(java.util.Locale.ROOT));
            add(button);
        }
        setVisible(getChildren().findAny().isPresent());
    }
    public static String key(EconomyIssues.Kind kind) {
        return switch (kind) { case DELIVERY -> ProductionKeys.DELIVERY; case METAL -> ProductionKeys.SHORTAGE;
            case DEPOSIT -> ProductionKeys.DEPOSIT; case LABOR -> ProductionKeys.LABOR; };
    }
}
