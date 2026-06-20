# Handoff: Unternehmen Enterprise UI-Kit

Fachneutrale, wiederverwendbare Komponentenbibliothek im **Unternehmen Corporate Design**. Dieses Paket befähigt Claude Code, eine Enterprise-Anwendung zu bauen, die exakt wie die Referenz aussieht.

---

## 1. Über die Design-Dateien — bitte zuerst lesen

Die beigelegte HTML-Datei ist eine **Design-Referenz** (ein Prototyp, der Optik und Verhalten zeigt) — **kein** produktiv zu kopierender Code. Ihre Aufgabe ist es, diese Designs in der **Zielumgebung** mit deren etablierten Mustern, Komponenten und Konventionen **nachzubauen** (1:1 Look, aber sauberer Framework-Code).

**So betrachten Sie die Referenz:** Datei `Enterprise UI-Kit.html` zusammen mit `support.js` (gleicher Ordner) **im Browser öffnen** — die komplette Bibliothek rendert live (Schriften via Google Fonts). Links die Sektions-Navigation, rechts der Inhalt. Sie ist die **pixelgenaue Quelle der Wahrheit** für Maße, Farben, Abstände und Typografie.

**Fidelity:** High-Fidelity. Farben, Typografie, Spacing, Radien und Zustände sind verbindlich und exakt mit `tokens.scss` umzusetzen.

---

## 2. Corporate Design (verbindlich)

Aus dem Unternehmen-Styleguide (Stand März 2026):

- **Unternehmen-Blau `#006EC7`** — immer **100 %**. **Keine Blau-Tints/Abstufungen.** Aktiv-/Hover-/Auswahl-Flächen werden über warmes **Unternehmen-Grau** (`--c-tint-blue` = `#F2F0EE`) gelöst, der Akzent (Text/Icon/Rand/Balken) bleibt volles Blau. Primärbutton-Hover wird *dunkler* (`#005AA3`), nie aufgehellt.
- **Unternehmen-Grau `#887D75`** (warm) ist die Neutralbasis; nur Grau darf abgestuft werden.
- **Schmuckfarben** (Grün/Türkis/Gelb/Orange/Pink/Violett) nur als sparsame Akzente (z. B. Avatare, Chart-Serien).
- **Schriften:** Fira Sans (UI/Headlines), Fira Mono (IDs/Zahlen/Tokens). *Klavika ist lizenzbeschränkt — nicht verwenden.*
- **Icons:** FontAwesome im Stil **„Thin"**, reine Outline, einheitliche Strichstärke (~1,2), in Hausfarben. Keine gefüllten Flächen. In der Referenz als SVG-Sprite (`<use>`) nachgebaut.
- **Logo:** In der Referenz nur typografisch angenähert. **Offizielles Logo/Favicon aus Celum** (`dam.Unternehmen-group.com` → Corporate Design) einsetzen; eigene Logo-Nachbauten sind laut CD nicht zulässig.

---

## 3. Layout-Prinzip & App-Shell

Standard: **persistente Navigation links, Inhalt rechts auf weißer Fläche.** Die Shell bleibt über alle Screens identisch; nur der Inhaltsbereich wechselt.

| Element | Maß (Token) | Verhalten |
|---|---|---|
| Sidebar | `--shell-sidebar` 248px (228px kompakt) | fix, scrollt nicht mit; Gruppen-Header (uppercase), aktiver Eintrag = `--c-tint-blue` Fläche + 3px Blau-Balken links + Text/Icon `--c-primary` |
| Topbar | `--shell-topbar` 56px | Titel/Kontext links, Suche + Aktionen + Avatar rechts |
| Inhalt | `--content-pad` 20px, max `--content-max` 1180px | weiß, scrollbar |
| Icon-Rail | `--shell-rail` 64px | Sidebar kollabiert < 1100px |

### Raster & Fluchten (verbindlich)

Damit Formulare und Filterleisten ruhig wirken, richtet sich alles an einem gemeinsamen Spaltenraster aus (Referenz-Sektion „★ Raster & Fluchten"):

- **12-Spalten-Raster**, durchgängiger **Gutter 16px** — keine wechselnden Abstände.
- Gleichartige Felder bekommen **gleiche Breite** über CSS-Grid (`grid-template-columns: repeat(n, 1fr)`) — **nie** Auto-Breite/`flex-wrap`-Chips bei mehreren Filtern.
- Felder einer Zeile teilen **eine Höhe** (40px) und Grundlinie; linke/rechte **Außenkanten fluchten**.
- Volle-Breite-Felder spannen alle Spalten (`grid-column: 1 / -1`) und brechen die Flucht nicht.
- Labels **linksbündig** über dem Feld, einheitlicher Abstand.
- Eine Toolbar-Filterzeile ist nur bei **wenigen** Filtern zulässig; bei vielen → Grid.

---

## 4. Komponenten-Katalog (Referenz-Sektionen → Bauteile)

Die Referenz ist in 16 Sektionen gegliedert. Jede ist als wiederverwendbare Komponente(n) umzusetzen.

**Grundlagen**
1. **Foundations** — Farb-Tokens, Typo-Skala, Spacing (4px), Radien, Elevation, Icon-Set. → globales Theme.

**Komponenten**
2. **Buttons & Aktionen** — `ButtonComponent` (Varianten: primär/sekundär/ghost/link/danger/success; Größen sm/md/lg; Zustände default/hover/focus/loading/disabled), Icon-Button, Button-Gruppe, Split-Button, Segmented Control.
3. **Formulare** — `FieldComponent` (Text, Platzhalter, Fokus, Fehler, Disabled, Textarea), `SelectComponent`, Such-Feld, Checkbox (inkl. indeterminate), Radio, Toggle/Switch, Slider, Datum, Dropzone/Upload mit Progress, Tags-Input/Multi-Select, Number-Stepper.
4. **Badges, Status & Avatare** — `BadgeComponent` (Status-Dot, Solid, Priorität), Tags, Counter, Mono-Chip, `AvatarComponent` (+ Status-Punkt, Gruppe/Stack).
4b. **Chips & Mehrfachauswahl** — `ChipComponent`: Tag-Chips (entfernbar, mit Farbpunkt), Auswahl-/Filter-Chips (Toggle), Mehrfachauswahl-Feld mit Vorschlags-Dropdown, Personen-/Avatar-Chips + Overflow („+5 weitere"), Zustände (Standard/Aktiv/Disabled/Hinzufügen).

**Tabellen**
5. **Tabellen & Filter** — `FilterBarComponent` (Suche, Facetten-Dropdowns mit Counter, „+ Filter", Spalten-/Export-Aktion, aktive-Filter-Chips mit „zurücksetzen"), `DataTableComponent` (Sortier-Header, Zeilenauswahl-Checkbox, **Bulk-Action-Toolbar** im Auswahl-Zustand, Fortschritts-/Status-Zellen, Zeilen-Menü), `PaginationComponent` (Seiten + Zeilen-pro-Seite), Empty-State.

**Widgets**
6. **Kacheln & Widgets** — **konfigurierbares Dashboard** (Drag-&-Drop-Raster + Tastatur-Alternative, Layout pro Nutzer/Rolle gespeichert, Widget-Katalog) mit Widget-Typen: Status/Health, Kennzahl, Mini-Tabelle, Nachrichten/Inbox, Aufgaben/To-do, Liste/Ranking, Aktivität, Kalender. Dazu `StatTileComponent`, Balken-/Linien-/Flächen-/gestapelte/Donut-Charts, **Ranking-Bars, Heatmap, Gauge** und eine **komplexe Auswertungstabelle** (Gruppierung, Inline-Bars, Sparkline + Trend je Zeile, Summen-Zeile).

**Navigation**
7. **Navigation** — Breadcrumb, Tabs (Underline + Pill), Stepper/Wizard, Sidebar-Item, Topbar, Pagination-Varianten.

**Feedback**
8. **Feedback & Overlays** — `AlertComponent` (info/success/warning/danger), Toast/Snackbar, Tooltip, Progress (Bar + Ring), `ModalComponent`/Dialog, Dropdown-Menü, Skeleton-Loader, Spinner.

**Daten-Darstellung**
9. **Listen, Timeline & Akkordeon** — Beschreibungs-Liste (Key/Value), Akkordeon/Collapsible, vertikale Timeline, Trend-/Vergleichs-Chart (Sparkline via SVG).
10. **Zustände & Fehlerseiten** — Empty / No-Results / Error-Karten; 404- und 403-Seiten.

**Seiten-Muster**
11. **App-Shell & Layout** — vollständiges Dashboard-Beispiel (Sidebar + Topbar + KPIs + Filter + Tabelle).
12. **Detail & Master-Detail** — Detailseite (Header mit Breadcrumb/Tabs/Aktionen, Eigenschaften-Grid, Seiten-Panel) + Split-View (Liste → Detail).
13. **Formular- & Einstellungsseite** — Settings-Sub-Nav, gruppierte Felder (Label-Spalte + Eingaben), Sticky-Footer (Speichern/Verwerfen).
14. **Board, Kalender & Onboarding** — Kanban (Spalten, Karten **gleicher Mindesthöhe** für ein ruhiges Bild, WIP-Counter), Kalender mit **Monats- und Tagesansicht** (Zeitraster, ganztägige + zeitgebundene Termine, Jetzt-Linie), Onboarding-Checkliste mit Fortschritt.
15. **Command-Palette & Panels** — Command-Palette (⌘K), Benachrichtigungs-Panel, Filter-Drawer (Seiten-Panel mit Overlay).
16. **Login & Pricing** — Login-Karte (inkl. SSO) + Plan-/Pricing-Karten (3 Stufen, „empfohlen"-Hervorhebung).

**Konzept (nicht-visuelle Spezifikation)**
17. **Informationsarchitektur** — Sitemap, max. 2 Navigationsebenen, rollen-gescopt, Breadcrumb ab Ebene 2, URL spiegelt Hierarchie.
18. **User Flows** — Kernstrecken (Anlegen, Finden & Bearbeiten, Mehrfachauswahl & Export) inkl. Abbruch-/Auto-Save-Verhalten.
19. **Responsive & Breakpoints** — SM/MD/LG/XL, Transformationsregeln (Sidebar→Rail→Overlay, Tabelle→Karten, Filter→Drawer, Touch ≥ 44px) inkl. **iPhone- und iPad-Mockups** (Bottom-Tab mobil, Icon-Rail auf Tablet). Layout an Container-Breite, nicht Viewport.
20a. **Theming Dark Mode** — vollständige **Dark-Mode-Vorschau** mit Komponenten-Parität; Umschaltung über `[data-theme="dark"]` / `prefers-color-scheme`, Unternehmen-Blau für Kontrast leicht aufgehellt.
20. **Barrierefreiheit (WCAG AA + 2.2)** — Fokus-Ring, Tastatur-Map, Focus-Trap, Kontrast-Nachweise, ARIA-Rollen, Status nie nur über Farbe. **Konformität BFSG/EN 301 549** (seit 28.06.2025 verpflichtend): WCAG-2.2-Kriterien (Zielgröße ≥ 24px, Fokus nicht verdeckt, **Drag-Alternative** für Kanban/Slider, barrierefreie Auth, konsistente Hilfe, redundante Eingabe vermeiden) + Barrierefreiheitserklärung & Feedback-Mechanismus.
21. **UX-Writing & Formate** — Tonalität (Verb+Objekt, Sie-Form), Fehlermeldungs-Muster, de-DE-Formate (Datum/Zeit/Währung/Zahl).
22. **Datenzustände & Validierung** — 6 Zustände je Ansicht (Leer/Laden/Gefüllt/Kein-Treffer/Fehler/Overflow), Inline- vs. Summary-Validierung.
23. **Rollen, Rechte & Benachrichtigungen** — Rechte-Matrix (ausblenden statt deaktivieren), Kanal-Wahl Toast vs. Inbox vs. Banner.
24. **Theming & Governance** — Hell/Dunkel über Token-Umschaltung, Naming, SemVer, Beitrags-/Review-Regeln. **Whitelabel/Brand-Theming:** schmale Brand-Token-Ebene (`--brand-primary/-neutral/-accent/-font/-radius`, kundeneditierbar), Komponenten-Tokens leiten ab; Laufzeit-Theming per `[data-tenant]`; Theme-Editor mit Live-Vorschau; **Kontrast-Guardrail (AA erzwungen)** + Schrift-Lizenz/Fallback.
25. **Einbettung & Embedded-Modus** — Chrome-Modi `full`/`embedded`/`bare` per Config (`--chrome-mode` / `?chrome=`); jede Detailseite eigenständig deep-linkbar; Container-Queries; iframe-Auto-Höhe + `postMessage`; Token/SSO-Durchreichung; gekapselte CSS-Tokens.

---

## 4a. Verbindliche Konzept-Vorgaben (Kurzfassung)

- **IA:** max. 2 Nav-Ebenen; Admin-Bereiche rollenabhängig **ausblenden**; Breadcrumb ab Ebene 2.
- **Responsive:** Sidebar < 1100 → Icon-Rail, < 640 → Overlay; Tabellen → Karten; Filter → Drawer; Touch-Targets ≥ 44px.
- **A11y:** sichtbarer Fokus überall, Focus-Trap in Modals, vollständige Tastaturbedienung, Kontrast AA, Status mit Icon+Text.
- **Validierung:** Inline onBlur, Summary beim Absenden, Primäraktion erst bei Gültigkeit aktiv.
- **Writing/Formate:** Sie-Form, Verb+Objekt auf Buttons, Fehlermeldung = Ursache + nächster Schritt, de-DE-Zahlen/Datum/Währung.
- **Benachrichtigungen:** Toast (eigene Aktion) · Inbox (von anderen) · Banner (blockierend/systemweit).
- **Theming/Governance:** Dark Mode per Token-Werte; Tokens = einzige Quelle; SemVer + Review für Systemänderungen.
- **Einbettung:** App-Shell rendert bedingt (`--chrome-mode`: full/embedded/bare); jede Detailseite ist eine eigenständige, deep-linkbare Route (`/…/:id`), lädt Daten selbst per ID, hat eigene Lade-/Fehler-/403-Zustände; Layout an Container-Breite (Container Queries); iframe-Auto-Höhe + `postMessage`; CSS-Tokens gekapselt.

---

## 5. Empfohlenes Komponenten-Mapping (Angular-Beispiel)

> Der konkrete Stack ist frei wählbar — nutzen Sie die Konventionen Ihres Repos. Beispiel für Angular (Standalone-Components, OnPush):

| Bauteil | Komponente |
|---|---|
| Shell + RouterOutlet | `AppShellComponent` |
| Sidebar-Eintrag | `NavItemComponent` |
| Button (alle Varianten) | `ButtonComponent` / `[appButton]`-Directive |
| Eingaben | `FieldComponent`, `SelectComponent`, `CheckboxComponent`, `ToggleComponent`, `SliderComponent`, `UploadComponent` |
| Badge / Avatar | `BadgeComponent`, `AvatarComponent` |
| Filterleiste / Tabelle | `FilterBarComponent`, `DataTableComponent`, `PaginationComponent` |
| Widgets | `StatTileComponent`, `ChartCardComponent`, `ActivityFeedComponent` |
| Feedback | `AlertComponent`, `ToastService`, `ModalComponent`, `TooltipDirective`, `DropdownComponent` |
| Seiten | `DetailPageComponent`, `SettingsPageComponent`, `KanbanBoardComponent`, `CalendarComponent`, `CommandPaletteComponent` |

---

## 6. Verbindliche UI-Regeln (Checkliste)

- [ ] Tokens als `tokens.scss` global einbinden — **einzige** Quelle der Wahrheit (keine Hex-Werte in Komponenten).
- [ ] Fonts: Fira Sans + Fira Mono (Google Fonts oder Intranet).
- [ ] Icons: FontAwesome „Thin", nur Outline, einheitliche Strichstärke, in Hausfarben (als ein SVG-Sprite mit `<use>`).
- [ ] Unternehmen-Blau nie aufhellen/tinten; Aktiv-Zustände über warmes Grau + voller Blau-Akzent.
- [ ] Fokus-Ring `--shadow-focus` an allen interaktiven Elementen; Hit-Targets ≥ 40px; Kontrast AA.
- [ ] Standardhöhen: Felder/Buttons 40px (md), 32px (sm), 48px (lg).
- [ ] Radien: Buttons/Felder 9px, Karten 12px, Sektions-Container 16px, Status-Badges pill.
- [ ] **Fluchten:** gleichartige Felder gleich breit (CSS-Grid, kein flex-wrap), gemeinsame Außenkanten, eine Zeilenhöhe (40px), Gutter 16px; volle Breite via `grid-column: 1 / -1`.
- [ ] Responsiv: Sidebar < 1100px → Icon-Rail 64px.
- [ ] Jede Liste/Tabelle hat definierte Zustände: Default, Laden (Skeleton), Empty, Fehler.
- [ ] **A11y / BFSG (seit 28.06.2025):** WCAG 2.1 AA + 2.2 — Zielgröße ≥ 24px, Fokus nicht verdeckt, Drag-Alternative (Kanban/Slider/Range), barrierefreie Auth (kein Captcha-Only), konsistente Hilfe, redundante Eingabe vermeiden; Barrierefreiheitserklärung + Feedback-Mechanismus.
- [ ] Logo/Favicon aus Celum (nicht den Platzhalter übernehmen).

---

## 7. Dateien in diesem Paket

- `Enterprise UI-Kit.html` — Design-Referenz (alle Sektionen, Foundations → Konzept → Embedded). Mit `support.js` im Browser öffnen.
- `support.js` — Runtime, **nur** damit die Referenz rendert (nicht in die App übernehmen).
- `tokens.scss` — Design-Tokens für die Umsetzung.

---

## 8. So gehen Sie mit Claude Code vor

1. Diesen Ordner ins (oder neben das) App-Repo legen.
2. Prompt an Claude Code, sinngemäß:
   > „Lies `design_handoff_enterprise_uikit/README.md` und `tokens.scss`. Öffne die HTML-Referenz als visuelle Vorlage. Richte zuerst das Token-Theme und die App-Shell ein, dann baue die Komponenten aus dem Katalog (Abschnitt 4) als wiederverwendbare Komponenten unserer Codebase nach — CD-konform, pixelgenau zur Referenz. HTML nicht kopieren, in unsere Framework-Konvention übersetzen."
3. Reihenfolge: **Tokens → App-Shell → Basis-Komponenten (Buttons, Felder, Badges) → Tabelle/Filter → Widgets → Seiten-Muster.**
4. Komponenten gegen die Referenz prüfen (gleiche Maße/Farben), dann projektspezifische Inhalte einsetzen.
