# language: de
Funktionalität: Regressionen aus dem Release-Review

  @map-interaction
  Szenario: Kleine Systeme bleiben zwischen vielen Flotten und kreuzenden Strecken erreichbar
    Wenn ich mich als "Map Targets" mit der E-Mail-Adresse "map-targets@example.test" registriere
    Und ich den Bestätigungslink für "map-targets@example.test" öffne
    Und ich mich mit "map-targets@example.test" anmelde
    Und zwei deterministische Karten für "map-targets@example.test" bereitstehen
    Dann bleiben Systeme zwischen dreißig Flotten bei verschiedenen Zoomstufen erreichbar

  @fleet-context @system-details
  Szenario: Ziehen, Kartenpositionen, Tastatur und erhaltene Eingaben funktionieren zusammen
    Wenn ich mich als "Review Map" mit der E-Mail-Adresse "review-map@example.test" registriere
    Und ich den Bestätigungslink für "review-map@example.test" öffne
    Und ich mich mit "review-map@example.test" anmelde
    Und zwei deterministische Karten für "review-map@example.test" bereitstehen
    Dann zeigt die Systemsicht direkte Aktionen in beiden Sprachen auf Desktopbreiten
    Und kann ich eine Verlegung per Ziehen erstellen
    Und bleiben Zoom und Kartenposition für beide Partien getrennt
    Und bleiben Eingaben bei Aktualisierungen erhalten und ist die Karte per Tastatur bedienbar

  Szenario: Fremde Chat-Nachrichten erscheinen live und ein Entwurf bleibt erhalten
    Wenn ich mich als "Review Chat Host" mit der E-Mail-Adresse "review-chat-host@example.test" registriere
    Und ich den Bestätigungslink für "review-chat-host@example.test" öffne
    Und ich mich als "Review Chat Guest" mit der E-Mail-Adresse "review-chat-guest@example.test" registriere
    Und ich den Bestätigungslink für "review-chat-guest@example.test" öffne
    Und ich mich mit "review-chat-host@example.test" anmelde
    Und zwei Browsersitzungen eine Partie für "review-chat-host@example.test" und "review-chat-guest@example.test" öffnen
    Dann erscheinen fremde Chat-Nachrichten auch nach erneutem Öffnen live

  Szenario: Die Fortsetzung informiert beide Spieler und das zweite Spielende wartet wieder auf Schlachten
    Wenn ich mich als "Review Winner" mit der E-Mail-Adresse "review-winner@example.test" registriere
    Und ich den Bestätigungslink für "review-winner@example.test" öffne
    Und ich mich als "Review Loser" mit der E-Mail-Adresse "review-loser@example.test" registriere
    Und ich den Bestätigungslink für "review-loser@example.test" öffne
    Und ich mich mit "review-winner@example.test" anmelde
    Und zwei Browsersitzungen eine Partie für "review-winner@example.test" und "review-loser@example.test" öffnen
    Dann hat ein Sieg mit 0:0 Restschiffen die richtige Farbe und Siegschwelle
    Wenn der Sieger bis zur vollständigen Eroberung weiterspielt
    Dann schließen beide Ergebnisdialoge und die Rundenfrist beginnt neu
    Und wartet das zweite Spielende in beiden Sitzungen wieder auf die offenen Schlachten
