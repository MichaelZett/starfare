# language: de
Funktionalität: Regressionen aus dem Release-Review

  @map-interaction
  Szenario: Kleine Systeme bleiben zwischen vielen Flotten und kreuzenden Strecken erreichbar
    Wenn das Konto "Map Targets" mit der E-Mail-Adresse "map-targets@example.test" besteht
    Und ich mich mit "map-targets@example.test" anmelde
    Und zwei deterministische Karten für "map-targets@example.test" bereitstehen
    Dann bleiben Systeme zwischen dreißig Flotten bei verschiedenen Zoomstufen erreichbar

  @fleet-context @system-details
  Szenario: Ziehen, Kartenpositionen, Tastatur und erhaltene Eingaben funktionieren zusammen
    Wenn das Konto "Review Map" mit der E-Mail-Adresse "review-map@example.test" besteht
    Und ich mich mit "review-map@example.test" anmelde
    Und zwei deterministische Karten für "review-map@example.test" bereitstehen
    Dann zeigt die Systemsicht direkte Aktionen in beiden Sprachen auf Desktopbreiten
    Und kann ich eine Verlegung per Ziehen erstellen
    Und bleiben Zoom und Kartenposition für beide Partien getrennt
    Und bleiben Eingaben bei Aktualisierungen erhalten und ist die Karte per Tastatur bedienbar
