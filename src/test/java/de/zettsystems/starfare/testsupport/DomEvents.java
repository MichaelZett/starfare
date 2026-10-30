package de.zettsystems.starfare.testsupport;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Input;
import tools.jackson.databind.node.JsonNodeFactory;

/** Client-originated events for native controls that have no dedicated tester. */
public final class DomEvents extends ComponentTester<Component> {

    private DomEvents(Component component) {
        super(component);
    }

    public static void fire(Component component, String type) {
        DomEvents events = new DomEvents(component);
        events.fireDomEvent(type);
        events.roundTrip();
    }

    public static void changeRange(Input input, int value) {
        DomEvents events = new DomEvents(input);
        events.ensureComponentIsUsable();
        events.setPropertyAsUser("value", String.valueOf(value));
        events.fireDomEvent("change", JsonNodeFactory.instance.objectNode()
                .put("element.value", String.valueOf(value)));
        events.roundTrip();
    }
}
