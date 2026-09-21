# language: de
Funktionalität: Rundenstatus

  Szenario: Mitspieler sehen Abgaben und die Nachzügler-Uhr
    Wenn ich mich als "Runden Host" mit der E-Mail-Adresse "rounds-host@example.test" registriere
    Und ich den Bestätigungslink für "rounds-host@example.test" öffne
    Und ich die Browsersitzung wechsle
    Und ich mich als "Runden Gast" mit der E-Mail-Adresse "rounds-guest@example.test" registriere
    Und ich den Bestätigungslink für "rounds-guest@example.test" öffne
    Und eine laufende Partie für "rounds-host@example.test" und "rounds-guest@example.test" mit Standardfristen bereitsteht
    Und ich mich mit "rounds-guest@example.test" anmelde
    Und ich die Partie öffne
    Dann zeigt die Rundenleiste 2 Spieler, davon 0 mit Haken
    Und läuft die Uhr bis zum Rundenende
    Wenn ich die nächste Runde auslöse
    Dann zeigt die Rundenleiste 2 Spieler, davon 1 mit Haken
    Und läuft die Nachzügler-Uhr bei "Host-Reich"
    Wenn ich die Browsersitzung wechsle
    Und ich mich mit "rounds-host@example.test" anmelde
    Und ich die Partie öffne
    Dann zeigt die Rundenleiste 2 Spieler, davon 1 mit Haken
    Und läuft die Nachzügler-Uhr bei "Host-Reich"
