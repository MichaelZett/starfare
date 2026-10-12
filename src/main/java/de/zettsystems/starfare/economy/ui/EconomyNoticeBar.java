package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import de.zettsystems.starfare.economy.values.EconomyIssues;
import de.zettsystems.starfare.i18n.I18n;
import java.util.List;
import java.util.function.Consumer;

/** Compact problem groups and a separate local-draft entry point. */
public final class EconomyNoticeBar extends Div {
    public EconomyNoticeBar() { addClassName("economy-notices"); }
    public void show(List<EconomyIssues.Issue> issues, int drafts, Consumer<EconomyIssues.Kind> open, Runnable openDrafts) {
        removeAll();
        for (var kind : EconomyIssues.Kind.values()) {
            long count = issues.stream().filter(issue -> issue.reasons().contains(kind)).count();
            if (count == 0) { continue; }
            Button button = new Button(I18n.t(key(kind), count), _ -> open.accept(kind));
            button.setId("economy-notice-" + kind.name().toLowerCase(java.util.Locale.ROOT));
            add(button);
        }
        if (drafts > 0) {
            Button button = new Button(I18n.t(ProductionKeys.DRAFTS, drafts), _ -> openDrafts.run());
            button.setId("production-drafts-open"); add(button);
        }
        setVisible(getChildren().findAny().isPresent());
    }
    public static String key(EconomyIssues.Kind kind) {
        return switch (kind) { case DELIVERY -> ProductionKeys.DELIVERY; case METAL -> ProductionKeys.SHORTAGE;
            case DEPOSIT -> ProductionKeys.DEPOSIT; case LABOR -> ProductionKeys.LABOR; };
    }
}
