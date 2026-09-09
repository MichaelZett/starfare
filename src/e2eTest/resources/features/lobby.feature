# language: de
Funktionalität: Private Lobby und Archiv

  Szenario: Sichtbarkeit, Einladung und lesende Nachbetrachtung
    Wenn ich mich als "Lobby Host" mit der E-Mail-Adresse "lobby-host@example.test" registriere
    Und ich den Bestätigungslink für "lobby-host@example.test" öffne
    Und ich mich mit "lobby-host@example.test" anmelde
    Und ich eine private Lobby-Partie ohne Beitritt anlege
    Und ich die Browsersitzung wechsle
    Und ich mich als "Lobby Guest" mit der E-Mail-Adresse "lobby-guest@example.test" registriere
    Und ich den Bestätigungslink für "lobby-guest@example.test" öffne
    Und ich mich mit "lobby-guest@example.test" anmelde
    Dann ist die private Lobby-Partie verborgen
    Wenn ich die Browsersitzung wechsle
    Und ich mich mit "lobby-host@example.test" anmelde
    Und ich die Partie im Verwalten-Dialog veröffentliche
    Und ich die Browsersitzung wechsle
    Und ich mich mit "lobby-guest@example.test" anmelde
    Dann ist die Lobby-Partie sichtbar
    Wenn der Host die Partie wieder privat stellt und "Lobby Guest" einlädt
    Und ich die Lobby-Einladung annehme
    Dann ist die Lobby-Partie sichtbar
    Wenn die Lobby-Partie regulär beendet wird
    Dann liegt die Partie nur im Archiv und lässt sich lesend öffnen
