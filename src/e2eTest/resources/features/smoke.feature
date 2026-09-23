# language: de
Funktionalität: Smoke — vom Konto bis zur ersten Runde
  Ein neuer Spieler legt sein Konto an, bestätigt die E-Mail-Adresse, meldet
  sich an, legt eine KI-Partie an und spielt die erste Runde. Deckt den Happy Path
  durch Identity-Baustein, Lobby, automatischen Start, Karte und Rundenbericht ab.

  Szenario: Registrieren, bestätigen, anmelden, Spiel anlegen und eine Runde spielen
    Wenn ich mich als "Alice Tester" mit der E-Mail-Adresse "alice@example.test" registriere
    Dann liegt eine Bestätigungsmail für "alice@example.test" vor
    Wenn ich den Bestätigungslink für "alice@example.test" öffne
    Und ich mich mit "alice@example.test" anmelde
    Dann sehe ich in der Lobby den Online-Spieler "Alice Tester"
    Wenn ich ein Standardspiel anlege und beitrete
    Dann zeigen die Empire-Stats 1 System, Produktion 4 und 8 Schiffe
    Wenn ich die nächste Runde auslöse
    Dann sehe ich den Rundenbericht für Runde 1
