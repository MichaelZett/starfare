# Wirtschaftsprüfung (Zusammenfassung)

Stand 25.11.2026: Die erweiterten Regeln wurden als Fassung 2.0.0 (technisch `spaceward/2.0.0`) neu gebaut. Von
der Wirtschaft blieb nur der **Industrieausbau**: Jedes System teilt seine Kapazität zwischen
Schiffbau und Ausbau. Metall, Förderung, Recycling, Förderautomatik, Kolonien und Schiffsforschung
sind entfallen, ebenso die Studien-Modi des Prüfstands (`investment`, `economy`, `research`).
Die ausführlichen Studien W4 bis W7 (Oktober/November 2026) und die Forschungsstudie stehen in der
Git-Historie dieser Datei und von `docs/RESEARCH-EXPERIMENTS.md` (bis Commit `4c2eaa2`).

## Was davon gilt

**Kostenkurve (W7).** Die nächste Ausbaustufe kostet bei Kapazität C
`min(2C, max(2, ceil(C²/10)))` Punkte (C=2 → 2, C=5 → 3, C=10 → 10, C=14 → 20, C=20 → 40,
ab C=20 wie früher 2C), höchstens Kapazität 50. Grund aus den Studien: Gegen frühe Flotten (Rush)
lohnte Ausbau nur, wenn die ersten Stufen günstig sind; ein pauschal günstigerer Ausbau auch später
brachte keinen Vorteil. Mit der W7-Kurve lag der erste fertige frühe Ausbau im Median bei Runde 3
(bei 2C-Preisen Runde 10), und früher Einzelausbau schlug gegen Rush und Konzentration die
Referenz ohne Ausbau (23/5/4 gegen 18/10/4 bzw. 14/9/9 gegen 12/12/8 Siege/Niederlagen/offen; die
unabhängige Kontrolle bestätigte die Richtung bei kleiner Stichprobe).

**Später Ausbau.** Bei C=25 kostet eine Stufe 50, bei C=45 90 Punkte; selbst wenn jede Stufe danach
jede Runde ein Schiff mehr liefert, dauert der Ausgleich 50 bis 90 Runden. Eine Regel „ab Runde X
nur noch Flotte“ gibt es nicht; es zählen Preis des Projekts, Bedrohung, Weg zur Front und
Resthorizont.

**Verworfene Stellgrößen.** Ein zusätzlicher Eignungsfaktor je System verbesserte den frühen Ausbau
nicht; halbierte Industrie nach Eroberung erzeugte mehr offene Partien (75 statt 43 von 256) ohne
klaren Vorteil. Beides ist nicht übernommen.

## Computerstärke

Die Stufen für Erweitert 2.0.0 sind mit dem Stärke-Prüfstand kalibriert
(`docs/STRATEGY-EXPERIMENTS.md`, Abschnitt „Computerstärke Erweitert (E5)“): Profile ohne
Industrieausbau verlieren dort fast immer gegen die ausbauende Basisstrategie, die adaptive
Industrie ist das stärkste Profil.
