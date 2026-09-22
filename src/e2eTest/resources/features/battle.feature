# language: de
Funktionalität: Schlachten vor dem Spielende auswerten

  Szenario: Offene Schlachten verbergen Sieg und offene Karte auch nach dem Neuladen
    Wenn ich mich als "Battle Tester" mit der E-Mail-Adresse "battle@example.test" registriere
    Und ich den Bestätigungslink für "battle@example.test" öffne
    Und ich mich mit "battle@example.test" anmelde
    Und eine Partie mit zwei abschließenden Schlachten für "battle@example.test" bereitsteht
    Dann bleiben Sieg und offene Karte bis zur letzten Schlacht verborgen
    Und kann ich die Kartenbreite durch Ziehen ändern
    Wenn ich beide Schlachten mit Ton auswerte
    Dann erscheint das Spielergebnis vor der offenen Nachbetrachtung
