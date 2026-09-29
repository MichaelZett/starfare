package de.zettsystems.starfare.legal.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

/** Concise, public explanation of the data processed by the game. */
@Route("privacy")
@PermitAll
public class PrivacyView extends VerticalLayout {
    public PrivacyView() {
        setMaxWidth("58rem");
        add(new H1("Datenschutz"),
                new Paragraph("Starfare verarbeitet Kontodaten des Anmeldedienstes, den gewählten Anzeigenamen, "
                        + "Partie- und Zugdaten sowie Nachrichten in einer Partie. E-Mail-Adresse und Passwort "
                        + "werden ausschließlich vom Anmeldedienst verarbeitet."),
                new H2("Zweck und Speicherdauer"),
                new Paragraph("Die Daten dienen dem Betrieb der Partien, der Anzeige von Ergebnissen und der "
                        + "Kommunikation innerhalb einer Partie. Direktnachrichten und Partie-Chats werden nach "
                        + "365 Tagen entfernt. Beendete Partien und ihre Statistik bleiben für die Nachbetrachtung gespeichert."),
                new H2("Deine Rechte"),
                new Paragraph("Du kannst dein Konto einschließlich der beim Anmeldedienst gespeicherten Kontodaten "
                        + "selbst löschen. Bereits abgeschlossene Partien bleiben als Spielhistorie erhalten; der "
                        + "personenbezogene Anzeigename wird dort nicht weiter mit einem Konto verknüpft."));
    }
}
