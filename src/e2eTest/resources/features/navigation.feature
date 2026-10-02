# language: de
Funktionalität: Spaceward-Reichweite und Zwischenstationen

  Szenario: Routen, Auftanken und unterbrochene Weiterreise bleiben nach Neuladen verständlich
    Wenn ich mich als "Navigator" mit der E-Mail-Adresse "navigation@example.test" registriere
    Und ich den Bestätigungslink für "navigation@example.test" öffne
    Und ich mich mit "navigation@example.test" anmelde
    Und eine interne Spaceward-Navigationspartie für "navigation@example.test" bereitsteht
    Dann zeigt der Flottenversand Zwischenstationen und lehnt unerreichbare Ziele ab
    Und bleibt der Auftankaufenthalt samt Ankunftsrunde nach Neuladen erhalten
    Und zeigt die Karte bei Stationsverlust eine unterbrochene Weiterreise ohne erfundene Ankunft
