package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.html.Div;
import de.zettsystems.starfare.style.HtmlAttributes;

/** Gives custom click targets the same activation keys as native buttons. */
final class KeyboardActions {
    private KeyboardActions() { }

    static void enable(Div target) {
        target.getElement().setAttribute(HtmlAttributes.ROLE, "button");
        target.getElement().setAttribute(HtmlAttributes.TABINDEX, "0");
        target.getElement().executeJs("""
                if (!this.__keyboardAction) {
                    this.__keyboardAction = true;
                    this.addEventListener('keydown', event => {
                        if (event.target !== this || event.repeat) return;
                        if (event.key === 'Enter' || event.key === ' ') {
                            event.preventDefault();
                            this.click();
                        }
                    });
                }
                """);
    }
}
