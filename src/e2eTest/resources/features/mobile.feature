# language: de
@ui-acceptance @mobile
Funktionalität: Handy-Oberfläche

  Szenario: Ein Handy bekommt seine eigene Oberfläche und bleibt per Fingertipp bedienbar
    Wenn ich mich als "Handy Host" mit der E-Mail-Adresse "mobile-host@example.test" registriere
    Und ich den Bestätigungslink für "mobile-host@example.test" öffne
    Und ich die Browsersitzung wechsle
    Und ich mich als "Handy Gast" mit der E-Mail-Adresse "mobile-guest@example.test" registriere
    Und ich den Bestätigungslink für "mobile-guest@example.test" öffne
    Und eine laufende Handy-Partie für "mobile-host@example.test" und "mobile-guest@example.test" bereitsteht
    Wenn ich mich auf dem Handy mit "mobile-host@example.test" anmelde
    Dann lässt sich der Spielassistent auf dem Handy bis zur Fußleiste bedienen
    Wenn ich die Partie auf dem Handy öffne
    Dann zeigt das Handy die eigene Kartenansicht und der Desktop die geteilte Ansicht
    Wenn ich auf dem Handy mein Heimatsystem antippe
    Dann öffnet sich auf dem Handy die Übersicht mit den Systemdetails
    Wenn ich auf dem Handy „Flotte senden“ wähle
    Dann zeigt die Handy-Karte den Zielhinweis und die Zielwahl lässt sich abbrechen
    Und kann ich auf dem Handy die Runde abgeben

  @research
  Szenario: Forschungsbudget bleibt auf dem Handy erreichbar und gespeichert
    Wenn ich die Schiffsforschung auf dem Handy prüfe
