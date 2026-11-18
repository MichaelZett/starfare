# language: de
@route-acceptance
Funktionalität: Routen mit langen Namen und unbekannten Stationen

  Szenario: Routen bleiben lesbar, bedienbar und ohne verborgene Systemdaten
    Wenn das Konto "Routes" mit der E-Mail-Adresse "routes@example.test" besteht
    Und ich mich mit "routes@example.test" anmelde
    Dann prüfe ich lange und unbekannte Routen in beiden Sprachen und Bildschirmgrößen
