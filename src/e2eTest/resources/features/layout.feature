# language: de
@ui-acceptance @layout
Funktionalität: Layout-Sicherung beim Umzug auf Browserless
  Szenario: Lobby, Wizard, Systemsicht und Archiv bleiben auf Desktopbreiten bedienbar
    Wenn ich den Layout-Rundgang bei 1366 und 1024 Pixeln durchlaufe
    Dann meldet die Browser-Konsole keine Fehler

  @economy-layout
  Szenario: Wirtschaftsdetails, Mondkolonie und Forschung passen auf beide Desktopbreiten
    Wenn ich Mondkolonie, Wirtschaftsplanung und Forschung bei beiden Desktopbreiten prüfe
    Dann meldet die Browser-Konsole keine Fehler
