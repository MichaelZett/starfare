# language: de
@ui-acceptance
@battle
Funktionalität: Schlachten vor dem Spielende auswerten

  Szenario: Offene Schlachten verbergen Sieg und offene Karte auch nach dem Neuladen
    Wenn das Konto "Battle Tester" mit der E-Mail-Adresse "battle@example.test" besteht
    Und ich mich mit "battle@example.test" anmelde
    Und eine Partie mit zwei abschließenden Schlachten für "battle@example.test" bereitsteht
    Dann bleiben Sieg und offene Karte bis zur letzten Schlacht verborgen
    Und kann ich die Kartenbreite durch Ziehen ändern
    Wenn ich beide Schlachten mit Ton auswerte
    Dann erscheint das Spielergebnis vor der offenen Nachbetrachtung
