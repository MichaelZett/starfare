# language: de
@ui-acceptance
Funktionalität: Smoke — vom Konto bis zur ersten Runde
  Ein Spieler mit einem Konto der Plattform meldet sich an, legt eine KI-Partie
  an und spielt die erste Runde. Deckt den Happy Path durch Lobby, automatischen
  Start, Karte und Rundenbericht ab.

  @direct-send
  Szenario: Anmelden, Spiel anlegen und eine Runde spielen
    Wenn das Konto "Alice Tester" mit der E-Mail-Adresse "alice@example.test" besteht
    Und ich mich mit "alice@example.test" anmelde
    Wenn ich ein Standardspiel anlege und beitrete
    Dann zeigen die Empire-Stats 1 System, Produktion 4 und 8 Schiffe
    Dann kann ich im Systeminspektor eine Flotte oder Verlegung vorbereiten und abbrechen
    Wenn ich die nächste Runde auslöse
    Dann sehe ich den Rundenbericht für Runde 1
    Und meldet die Browser-Konsole keine Fehler
