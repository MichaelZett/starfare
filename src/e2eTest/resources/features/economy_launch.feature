# language: de
@ui-decisions
@economy-launch
Funktionalität: Spaceward-Wirtschaft über den Spielassistenten
  Szenario: Eine neue Spaceward-Partie verwendet die konfigurierte Regelversion
    Wenn ich mich als "Economy Tester" mit der E-Mail-Adresse "economy@example.test" registriere
    Dann liegt eine Bestätigungsmail für "economy@example.test" vor
    Wenn ich den Bestätigungslink für "economy@example.test" öffne
    Und ich mich mit "economy@example.test" anmelde
    Dann prüfe ich die Spaceward-Neuanlage und die freigeschaltete Wirtschaft
