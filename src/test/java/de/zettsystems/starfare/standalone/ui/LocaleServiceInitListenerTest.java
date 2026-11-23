package de.zettsystems.starfare.standalone.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Locale;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LocaleServiceInitListenerTest {
    private final VaadinService service = mock(VaadinService.class);
    private final VaadinSession session = mock(VaadinSession.class);
    private final UI ui = mock(UI.class);
    private UIInitListener uiInit = _ -> { };

    @BeforeEach
    void registerOnTheService() {
        new LocaleServiceInitListener().serviceInit(new ServiceInitEvent(service));
        ArgumentCaptor<UIInitListener> listener = ArgumentCaptor.forClass(UIInitListener.class);
        verify(service).addUIInitListener(listener.capture());
        uiInit = listener.getValue();
    }

    @AfterEach
    void clearCurrentSession() {
        CurrentInstance.clearAll();
    }

    @Test
    void aNewUiGetsTheLocaleStoredInTheSession() {
        when(session.getAttribute(LocaleServiceInitListener.SESSION_KEY)).thenReturn(Locale.ENGLISH);
        VaadinSession.setCurrent(session);

        uiInit.uiInit(new UIInitEvent(ui, service));

        verify(ui).setLocale(Locale.ENGLISH);
    }

    @Test
    void withoutAStoredLocaleTheUiKeepsItsOwn() {
        when(session.getAttribute(LocaleServiceInitListener.SESSION_KEY)).thenReturn("en");
        VaadinSession.setCurrent(session);

        uiInit.uiInit(new UIInitEvent(ui, service));

        verifyNoInteractions(ui);
    }

    @Test
    void withoutASessionNothingHappens() {
        VaadinSession.setCurrent(null);

        uiInit.uiInit(new UIInitEvent(ui, service));

        verifyNoInteractions(ui);
    }
}
