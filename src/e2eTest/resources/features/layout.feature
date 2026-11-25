# language: de
@ui-acceptance @layout
Funktionalität: Layout-Sicherung beim Umzug auf Browserless
  Szenario: Lobby, Dialog „Neue Partie“, Systemsicht und Archiv bleiben auf Desktopbreiten bedienbar
    Wenn ich den Layout-Rundgang bei 1366 und 1024 Pixeln durchlaufe
    Dann meldet die Browser-Konsole keine Fehler

  @economy-layout
  Szenario: Industrie und Produktionsplanung passen auf beide Desktopbreiten
    Wenn ich die Industrie bei beiden Desktopbreiten prüfe
    Dann meldet die Browser-Konsole keine Fehler
