# language: de
Funktionalität: Getrennte Bündnisoptionen und gemeinsamer Sieg

  Szenario: Partieoptionen und gemeinsames Ergebnis bleiben nachvollziehbar
    Wenn das Konto "Commander" mit der E-Mail-Adresse "group-win@example.test" besteht
    Und ich mich mit "group-win@example.test" anmelde
    Dann prüfe ich die getrennten Bündnisoptionen in der internen Wizard-Vorschau
    Wenn eine interne Bündnissiegpartie für "group-win@example.test" endet
    Dann zeigt die Oberfläche beide Sieger auch nach Neuladen
    Und zeigt das Archiv den gemeinsamen Sieg
