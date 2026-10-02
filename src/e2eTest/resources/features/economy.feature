# language: de
Funktionalität: Spaceward-Industrieansicht

  Szenario: Produktionsverteilung, Ausbaufortschritt und Lieferengpässe bleiben konsistent
    Wenn ich mich als "Industrialist" mit der E-Mail-Adresse "industry@example.test" registriere
    Und ich den Bestätigungslink für "industry@example.test" öffne
    Und ich mich mit "industry@example.test" anmelde
    Und eine interne Spaceward-Testpartie für "industry@example.test" bereitsteht
    Dann kann ich den Schiffbau zugunsten der Industrie umschalten und sehe den Lieferengpass
    Und bleibt die Verteilung nach Neuladen erhalten und sperrt sich nach Abgabe
    Und erhöht sich die Kapazität nach fünf Runden ohne vorzeitige Schiffsproduktion
