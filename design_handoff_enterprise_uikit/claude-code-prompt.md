# Claude-Code-Prompt — Enterprise UI-Kit umsetzen (Neubau **oder** Bestand anpassen)

Dieses Konzept funktioniert für **beide** Fälle: ein neues Frontend von Grund auf **oder** eine bestehende Anwendung, die nur an das neue Design angeglichen wird. Kopiere den Ordner `design_handoff_enterprise_uikit/` ins Repo und gib Claude Code den folgenden Prompt — er erkennt selbst, welcher Fall vorliegt.

---

## Start-Prompt (kopieren)

> Du sollst dieses Frontend an das Design-Konzept in `design_handoff_enterprise_uikit/` angleichen. Es kann sich um ein **neues Projekt** oder eine **bestehende Anwendung** handeln — **prüfe zuerst, welcher Fall vorliegt**, und richte dein Vorgehen danach aus.
>
> **Immer zuerst lesen:** `README.md` (Regeln, Komponenten-Katalog, Konzept-Kapitel) und `tokens.scss` (einzige Quelle für Farben/Typo/Spacing/Radien). Öffne `Enterprise UI-Kit.html` im Browser als **pixelgenaue visuelle Referenz** — Vorlage, **nicht** zu kopierender Code.
>
> **Schritt 0 — Bestand sichten:**
> - Gibt es bereits nennenswerten UI-Code (Komponenten, Routen, globale Styles/Theme)? → **Bestands-Modus**.
> - Ist das Repo leer oder nur ein Gerüst? → **Neubau-Modus**.
> - Erstelle in **beiden** Fällen kurz einen Plan und beginne mit den Tokens.
>
> **Bestands-Modus (Anpassung, kein Neubau):**
> - Vorhandene Struktur, Routen, Daten- und Business-Logik **bleiben erhalten**; angepasst werden Optik, Komponenten und die Konzept-Muster.
> - Erst **Gap-Analyse**: vorhandene Komponenten ↔ Konzept-Bauteile (Button, Field, Badge, Chip, Tabelle/Filter, Widgets, App-Shell …); Abweichungen bei Farbe/Schrift/Spacing/Radien/Zuständen notieren. Migrationsplan in kleinen, prüfbaren Schritten vorschlagen und auf Freigabe warten, bevor du breit umbaust.
> - Komponenten **refactoren statt ersetzen**, API/Props möglichst beibehalten; Hardcodes schrittweise auf `var(--*)`. **Keine Regression** — pro Schritt lauffähig, getestet, gegen die Referenz geprüft. Alt auf Neu **mappen**, nichts parallel duplizieren.
>
> **Neubau-Modus:**
> - Token-Theme und App-Shell aufsetzen, dann Komponenten und Seiten-Muster gemäß Katalog neu bauen.
>
> **Gemeinsame Reihenfolge (inkrementell):**
> 1. **Tokens** als globales Theme einbinden — **keine Hex-Werte** in Komponenten, nur `var(--*)`.
> 2. **Grundlagen/Schrift** (Fira Sans / Fira Mono, Typo-Skala, Radien, Schatten).
> 3. **App-Shell** (Sidebar 248 / Topbar 56 / Inhalt), `--chrome-mode` (full/embedded/bare). Im Bestand: bestehende Navigation/Routen übernehmen, nur Optik/Verhalten angleichen.
> 4. **Basis-Komponenten:** Button, Field/Select/Checkbox/Toggle/Slider/Upload, Badge, **Chip**, Avatar.
> 5. **Tabelle + Filterleiste** (Sortierung, Auswahl, Bulk-Toolbar, Pagination, Empty/Skeleton).
> 6. **Widgets + konfigurierbares Dashboard** (Drag-&-Drop **mit** Tastatur-Alternative); Charts + Auswertungstabelle.
> 7. **Seiten-Muster:** Detail/Master-Detail, Formular/Settings, Board/Kalender (Monat+Tag), Command-Palette/Panels, Login/Pricing.
>
> **Verbindlich beachten** (Details im README):
> - Token-System; **Whitelabel** über schmale `--brand-*`-Ebene (Komponenten leiten ab).
> - **Barrierefreiheit BFSG / WCAG 2.1 AA + 2.2:** sichtbarer Fokus, volle Tastaturbedienung, Kontrast ≥ 4.5:1, Zielgrößen ≥ 24 px, Drag-Alternative, barrierefreie Auth, `aria-label` an Icon-Buttons, `role="img"`+Alternative an Diagrammen, `aria-busy` an Ladezuständen; **Kontrast-Guardrail erzwingen**.
> - **Responsive:** Sidebar < 1100 → Icon-Rail, < 640 → Overlay; Tabelle → Karten; Filter → Drawer; Touch ≥ 44 px; Layout an Container-Breite.
> - **Fluchten:** gleichartige Felder per CSS-Grid (kein flex-wrap), gemeinsame Außenkanten, eine Zeilenhöhe, Gutter 16; volle Breite via `grid-column: 1 / -1`.
> - **Tag/Nacht:** Dark Mode live über `[data-theme="dark"]` / `prefers-color-scheme`.
> - **Einbettung:** jede Detailseite eigenständig deep-linkbar (`/…/:id`), lädt Daten selbst, eigene Lade-/Fehler-/403-Zustände.
>
> Arbeite **komponentenweise** in kleinen Commits/PRs, jede Änderung gegen die Referenz geprüft (gleiche Maße/Farben). Im Bestands-Modus zusätzlich: bestehende Funktion nicht brechen. Frag nach, wenn etwas im Konzept oder im Bestand mehrdeutig ist.

---

## Vorgehen (Kurzform)
Bestand sichten → (Bestand: Gap-Analyse + Freigabe) → Tokens → Grundlagen/Schrift → App-Shell → Basis-Komponenten → Tabelle/Filter → Widgets/Dashboard → Seiten-Muster → Konzept-Regeln (A11y/Responsive/Embedded/Whitelabel) durchgehend.

## Leitplanken
- **Inkrementell, nicht „big bang"** — jeder Schritt lauffähig und prüfbar.
- **Neubau:** Katalog von oben nach unten aufbauen.
- **Bestand:** Logik/Datenflüsse unangetastet lassen, API/Props beibehalten, innen refactoren, visuelle Regression je Schritt prüfen, Alt→Neu mappen statt duplizieren.
- Referenz ist Maßstab für Maße/Farben/Abstände; Tokens sind die einzige Style-Quelle.

## Assets, die noch gebraucht werden
- Offizielles **Logo/Favicon** (Platzhalter in der Referenz ersetzen).
- **Fira Sans + Fira Mono** (Google Fonts oder Intranet); bei Kundenschrift Lizenz + Fallback-Stack.
