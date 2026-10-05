# language: de
Funktionalität: Getrennte Bündnisoptionen und gemeinsamer Sieg

  Szenario: Partieoptionen und gemeinsames Ergebnis bleiben nachvollziehbar
    Wenn ich mich als "Commander" mit der E-Mail-Adresse "group-win@example.test" registriere
    Und ich den Bestätigungslink für "group-win@example.test" öffne
    Und ich mich mit "group-win@example.test" anmelde
    Dann prüfe ich die getrennten Bündnisoptionen in der internen Wizard-Vorschau
    Wenn eine interne Bündnissiegpartie für "group-win@example.test" endet
    Dann zeigt die Oberfläche beide Sieger auch nach Neuladen
    Und zeigt das Archiv den gemeinsamen Sieg
