# language: de
Funktionalität: Spaceward-Bündnisse und Kündigungsfrist

  Szenario: Zwei Imperien stimmen zu und sehen die Kündigung mit anschließender Heimreise
    Wenn ich mich als "Diplomat" mit der E-Mail-Adresse "diplomat@example.test" registriere
    Und ich den Bestätigungslink für "diplomat@example.test" öffne
    Und ich mich als "Partner" mit der E-Mail-Adresse "partner@example.test" registriere
    Und ich den Bestätigungslink für "partner@example.test" öffne
    Und ich mich mit "diplomat@example.test" anmelde
    Und zwei Imperien die interne Bündnispartie für "diplomat@example.test" und "partner@example.test" öffnen
    Dann erfordert die Bündnisgründung die Zustimmung des Partners
    Und vereinbaren beide eine neue Kündigungsfrist ohne gemeinsame Schiffseigentümer
    Und bleibt die sichtbare Kündigung nach Neuladen erhalten und löst eine Heimreise aus
