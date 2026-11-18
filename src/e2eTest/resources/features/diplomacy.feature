# language: de
@diplomacy
Funktionalität: Spaceward-Bündnisse und Kündigungsfrist

  Szenario: Zwei Imperien stimmen zu und sehen die Kündigung mit anschließender Heimreise
    Wenn das Konto "Diplomat" mit der E-Mail-Adresse "diplomat@example.test" besteht
    Und das Konto "Partner" mit der E-Mail-Adresse "partner@example.test" besteht
    Und ich mich mit "diplomat@example.test" anmelde
    Und zwei Imperien die interne Bündnispartie für "diplomat@example.test" und "partner@example.test" öffnen
    Dann erfordert die Bündnisgründung die Zustimmung des Partners
    Und vereinbaren beide eine neue Kündigungsfrist ohne gemeinsame Schiffseigentümer
    Und bleibt die sichtbare Kündigung nach Neuladen erhalten und löst eine Heimreise aus
