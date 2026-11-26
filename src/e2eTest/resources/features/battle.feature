# language: de
@ui-acceptance
@battle
Funktionalität: Schlachten vor dem Spielende auswerten

  Szenario: Schlachten mit Ton auswerten und danach das Spielergebnis sehen
    Wenn das Konto "Battle Tester" mit der E-Mail-Adresse "battle@example.test" besteht
    Und ich mich mit "battle@example.test" anmelde
    Und eine Partie mit zwei abschließenden Schlachten für "battle@example.test" bereitsteht
    Dann liegt der Rundenbericht als Vorhang vor dem Brett
    Wenn ich beide Schlachten mit Ton auswerte
    Dann erscheint das Spielergebnis vor der offenen Nachbetrachtung
    Und kann ich die Kartenbreite durch Ziehen ändern
