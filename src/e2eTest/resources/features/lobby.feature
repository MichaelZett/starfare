# language: de
@ui-acceptance
Funktionalität: Private Lobby und Archiv

  Szenario: Sichtbarkeit, Einladung und lesende Nachbetrachtung
    Wenn das Konto "Lobby Host" mit der E-Mail-Adresse "lobby-host@example.test" besteht
    Und das Konto "lobby-host@example.test" Tester für Starfare ist
    Und ich mich mit "lobby-host@example.test" anmelde
    Und ich eine private Lobby-Partie ohne Beitritt anlege
    Und ich die Browsersitzung wechsle
    Und das Konto "Lobby Guest" mit der E-Mail-Adresse "lobby-guest@example.test" besteht
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
    Und kann ich Perspektive und Kriegsnebel der Nachbetrachtung wechseln
    Und kann ich die Inhalte der Karten-Seitenleiste wählen
