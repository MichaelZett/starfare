# language: de
@route-acceptance
Funktionalität: Routen mit langen Namen und unbekannten Stationen

  Szenario: Routen bleiben lesbar, bedienbar und ohne verborgene Systemdaten
    Wenn ich mich als "Routes" mit der E-Mail-Adresse "routes@example.test" registriere
    Und ich den Bestätigungslink für "routes@example.test" öffne
    Und ich mich mit "routes@example.test" anmelde
    Dann prüfe ich lange und unbekannte Routen in beiden Sprachen und Bildschirmgrößen
