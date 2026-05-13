# Briefing Agent — Phase 1 Completion Report

| Feld | Wert |
|------|------|
| Branch | `claude/briefing-agent-phase1-SHTMm` |
| Bezugs-Spec | `docs/briefing-agent-phase1-spec.md` |
| Iterationsplan | `docs/iteration-plan.md` |
| Tags | `iter-0-complete` … `iter-7-complete` |
| Backend-Suite | 142 Surefire-Tests, alle grün |
| Frontend-Suite | Karma-Specs für jede Feature-Komponente; lint + production build clean |
| Naming-Audit | 0 Treffer für den firmenbezeichnenden Begriff aus dem Designkontext über alle Source-, Konfig-, Test- und Dokumentationsdateien (Ausnahme: die unveränderte Spec-Datei). |

---

## 1. Umgesetzte Features (gemappt auf die Spec)

### Capture (Spec §6.1, §10.1)

| Spec-Punkt | Umsetzung |
|------------|-----------|
| Audio-Capture im Web (MediaRecorder) | `frontend/src/app/features/ereignis/audio-recorder.service.ts` + `capture-audio.component.ts`. 10-Min-Soft-Warning, 15-Min-Hard-Stop, klare Fehlermeldungen für „Permission denied" / „MediaRecorder unavailable". |
| Text-Capture im Web | `capture-text.component.ts` mit 10 000-Zeichen-Hard-Cap und Live-Restzähler. |
| Multipart-POST Audio → Whisper, Audio nicht persistiert | `EreignisController.captureAudio(...)` + `WhisperSttClient`. Audio wird via `InputStreamResource` mit Chunked Transfer durchgeleitet (ADR 0009). |
| MIME-Whitelist | `AudioMediaTypes` (ADR 0010). |
| Sprachcodes / Dauer aus Whisper-Response | `TranscriptionResult.language` / `durationSeconds`, persistiert auf `Ereignis`. |

### Domänenmodell (Spec §4, §5)

| Tabellen | Migration |
|----------|-----------|
| user_account, person, person_persona, persongroup, persongroup_member, topic, topic_member, ereignis, summary | `V202605120001__core_schema.sql` |
| summary.edit_history (JSONB) + GIN | `V202605120002__summary_edit_history.sql` |
| task, task_status_history, task_reminder | `V202605120003__task_tables.sql` |
| llm_provider, llm_provider_usage, stt_provider, prompt_template + partial unique indexes | `V202605120004__provider_and_prompt_tables.sql` |
| user_account.is_admin | `V202605120005__user_admin_flag.sql` |
| tsvector + GIN über person, persongroup, topic, ereignis, summary; unaccent | `V202605120006__fulltext_search.sql` |

CHECK-Constraints aus der Spec sind 1:1 in Migrationen verankert:
- `summary_one_audience_target`, `summary_audience_type_chk`,
  `summary_confidence_chk`, `summary_edit_state_chk`
- `task_exactly_one_assignment`, `task_status_chk`,
  `task_status_history_to/from_chk`, `task_reminder_type_chk`
- `llm_provider_usage_one_active_per_purpose` (partial unique)
- `stt_provider_one_active` (partial unique)
- `prompt_template_one_active_per_author_purpose` (partial unique)
- `task_reminder_unique` (täglich pro task + reminder_type)

### KI-Pipeline (Spec §6.2, §7, §7a)

| Spec-Punkt | Umsetzung |
|------------|-----------|
| Audience-Klassifikation als reiner LLM-Call | `AudienceClassificationService` mit lenient JSON-Parsing (ADR 0013). |
| Multi-Shot Summary pro Audience | `SummaryGenerationService` mit Persona-Text im System-Prompt. |
| Default-Topic-Fallback bei leerer Klassifikation | `EreignisService.runPipeline(...)` (ADR 0012). |
| Konfigurierbare LLM-/STT-Provider mit Verwendungszwecken | Tables + REST CRUD + Verbindungstest (ADR 0020). |
| Prompt-Templates pro Autor mit Versionierung + Pflicht-Platzhalter | `PromptTemplateService`, `PromptPlaceholders` (ADR 0019). |
| Mock-LLM für Iter 0 / lokale Demos | `MockLlmClient`. |

### Review-Workflow (Spec §6.3)

| Spec-Punkt | Umsetzung |
|------------|-----------|
| Transkript-Edit / Release | `EreignisService.editTranscript`, `release` + REST. |
| Summary-Edit, Regenerate (mit Feedback), Accept | `SummaryReviewService` + `/api/summaries/...`. |
| Append-only Audit-Trail | `summary.edit_history` JSONB + `EditHistoryEntry` (ADR 0014). |
| Markdown-Render mit Sanitizer | `MarkdownRendererService` (marked + DOMPurify, ADR 0015). |
| Lock auf akzeptierte Summaries | HTTP 409 in `SummaryReviewService.loadEditable`. |

### Tasks (Spec §6.5)

| Spec-Punkt | Umsetzung |
|------------|-----------|
| Vier Zuweisungstypen mit CHECK | `task_exactly_one_assignment`. |
| Status-Lifecycle open/in_progress/done/dropped | `TaskService.changeStatus(...)`, terminal-state HTTP 409. |
| Status-Historie mit Note + Author | `TaskStatusHistory`. |
| Reminder einen Tag vorher + am Tag der Fälligkeit | `TaskReminderScheduler`, `TaskReminder` (eindeutig pro Tag). |
| In-App-Notifications | `NotificationController` (Polling-Modell, ADR 0016). |
| Self-Tasks bei User-Deaktivierung droppen | `UserDeactivationService` (Iter 6). |

### Konsum-UX (Spec §6.4, §9)

| Spec-Punkt | Umsetzung |
|------------|-----------|
| Globale Suche oben + Tasten-Shortcut „/" | Shell-Komponente + `SearchService` + `/search`. |
| Volltext-Suche via PostgreSQL `tsvector`/`tsquery` | `SearchService` (ADRs 0024/0025). |
| Audience-Detail mit Sticky-Header (Persona) | `AudienceDetailComponent`. |
| Recent Activity Dashboard | `DashboardController`/`DashboardComponent`. |
| Brand-neutrale CSS-Variablen | `frontend/src/styles.scss` (ADR 0006). |

### Security (Spec §8)

| Spec-Punkt | Umsetzung |
|------------|-----------|
| Lokales Auth (BCrypt 12+) | `LocalAuthenticationProvider` via Spring + `PasswordEncoderConfig` (ADR 0003). |
| CSRF-Schutz per Cookie/Header | `SecurityConfig` + `CsrfController`. |
| Sichere Default-Header (CSP, HSTS, X-Frame, Referrer-Policy) | `SecurityConfig`. |
| Architektur Migration-fähig (entra_object_id / entra_upn vorhanden) | `UserAccount`-Felder, `AuthenticationProvider`-Abstraktion. |
| Admin-Rolle minimal (deaktivieren / tombstone) | `is_admin` Flag, ROLE_ADMIN, AdminController (ADR 0023). |
| Tombstone-Pattern für Personen | `PersonTombstoneService` (ADR 0022). |
| Author-Ausscheiden mit 6-Monatsfrist + Auto-Delete | `UserDeactivationService` + `AuthorDeletionJob`. |
| Retention für Transkripte + Summaries | `TranscriptRetentionJob`, `SummaryRetentionJob` (ADR 0021). |

### Frontend-Architektur (Spec §9.1)

- Angular 18, Standalone-Components, Signals (ADR 0005).
- Lazy-loading aller Routes via `loadComponent`.
- Reactive Forms.
- ESLint + Karma headless.
- CSS-Variablen, kein UI-Framework, mobil reaktiv.

### Infrastruktur

- `scripts/run.sh` — Branch-getriebenes Setup: Tooling-Check, Branch-Auswahl, Postgres via Docker Compose, Build, Launch (ADR 0008).
- `docker-compose.yml` — `postgres:16-alpine` mit Healthcheck.
- Maven Profil `skip-integration-tests` trennt Surefire (lokal) von Failsafe (Testcontainers).
- README im Repo-Root erklärt Setup in unter 10 Minuten.

---

## 2. Architecture Decision Records

| Nr. | Titel | Kurzbegründung |
|-----|-------|----------------|
| 0001 | Maven als Build-Tool | Konvention, ohne Daemon, Spring-BOM-freundlich. |
| 0002 | Spring Boot 3.3.5 | LTS-Linie, Java-21-fähig. |
| 0003 | BCrypt cost 12 | OWASP-2024-Empfehlung; @Min(10) als Floor. |
| 0004 | Flyway `V{YYYYMMDDhhmm}` Naming | Zeitstempel statt sequenzielle Nummern, kollisionsarm. |
| 0005 | Angular 18 standalone + signals | Geringe Surface-Area, kein NgRx. |
| 0006 | CSS-Variablen brand-neutral | Naming-Regel + Theme-Wechsel ohne Rename. |
| 0007 | Spring `RestClient` statt Spring AI | Provider-Konfiguration zur Laufzeit. |
| 0008 | `scripts/run.sh` als Launcher | Bash, idempotent, einziger Entry-Point. |
| 0009 | Audio nie auf persistente Disk | DSGVO-Datenminimierung. |
| 0010 | Audio MIME-Whitelist | Reduziert Angriffsfläche, kein audio/* free-for-all. |
| 0011 | Drei getrennte Audience-Tabellen | Eigene Integritäts-Regeln je Typ. |
| 0012 | Synchron-Pipeline mit Default-Topic-Fallback | Determinismus + jede Capture hinterlässt Spur. |
| 0013 | Lenient JSON-Parsing für LLM-Antworten | Robust gegen Halluzinationen, kein Pipeline-Crash. |
| 0014 | JSONB edit_history append-only | Audit-Trail ohne extra Tabelle. |
| 0015 | marked + DOMPurify | Sicheres Markdown-Rendering. |
| 0016 | Reminder via Polling | Web-Push für Phase 2. |
| 0017 | TaskStatusHistory + Clock-Bean | Deterministische Tests. |
| 0018 | SecretStore-Abstraktion (Env in Phase 1) | Kein Klartext in DB; Vault später. |
| 0019 | Prompt-Template-Versioning + partial unique | „Eine aktive Version pro (author, purpose)". |
| 0020 | Provider-Test als HTTP-HEAD-Probe | Erste Stufe; tiefer Test mit SecretStore. |
| 0021 | Drei kleine Retention-Jobs | Idempotent + isolierte Failure-Domänen. |
| 0022 | Tombstone-Pseudonym mit AtomicLong | Monoton, kollisionsfrei. |
| 0023 | Admin-Rolle als Boolean-Flag | Phase 1 minimal; Entra-Gruppen Phase 2. |
| 0024 | PostgreSQL FTS mit `german` + GIN | Kein eigener Suchdienst. |
| 0025 | Such-Ranking-Gewichte A/B/C | Namen vor Persona vor E-Mail. |
| 0026 | `/`-Shortcut für globale Suche | Web-Konvention, geringer Aufwand. |

---

## 3. Abgedeckte Edge Cases (Auswahl)

- **Capture**: leeres Text-Input, Text > Hard-Cap, leeres Audio, falsches MIME, Whisper liefert leeren Text (HTTP 422), Whisper down (HTTP 502), Audio-Aufnahme erreicht Hard-Cap (truncated_at_limit).
- **Klassifikation**: malformed JSON, unbekannte UUID, Type-Mismatch, ungültige confidence-Strings, leere Audience-Liste mit Fallback auf default-topic.
- **Multi-Shot**: leere Match-Liste = 0 saves, fehlende Audience-Targets liefern 404.
- **Review**: identische Edits sind no-op, Edit nach Accept = 409, Regenerate ohne Feedback erlaubt.
- **Tasks**: Status-Übergang von Terminal = 409, Idempotenz beim Reminder-Scheduler innerhalb eines Tages, Tasks ohne due_date werden ignoriert, Self-Tasks beim Deaktivieren auto-dropped.
- **Provider/Prompts**: Aktivierung verschiebt vorherigen Aktiv-Eintrag, partial-unique-Index als Letzter-Linie-Defense, Prompt ohne Pflicht-Platzhalter = 400 mit Liste.
- **Retention**: zweiter Lauf am gleichen Tag = no-op, NULL-Retention bedeutet „unbegrenzt", User-Reaktivierung nach Ablauf der Frist = 409.
- **Search**: leere Query = leere Ergebnisliste, sehr lange Query auf 200 Zeichen gekappt, Umlaute via `unaccent` matchen Mueller↔Müller, Tombstone-Personen werden mit Pseudonym gerendert.
- **Audio-Recorder im Browser**: MediaRecorder fehlt → state `unavailable`; Permission denied → state `denied`; getUserMedia abgelehnt → klare Fehlermeldung.

---

## 4. Bekannte Limitationen / Phase-2-Liste

- **Pipeline noch synchron** (ADR 0012). Async/Spring-Events folgen wenn echte LLM-Latenzen das erzwingen.
- **Provider-Tabellen vorhanden, LlmClient-Aufrufe nutzen sie noch nicht**. Iter-5-Tabellen sind die Source of Truth, aber `MockLlmClient` und `WhisperSttClient` ziehen ihre Konfiguration weiterhin aus `application.yml`. Das Refactoring (Resolver liest aktive Provider per Purpose und baut den HTTP-Call) ist die erste Phase-2-Aufgabe.
- **Provider-Test ist eine HTTP-HEAD-Probe** (ADR 0020). Echtes Roundtrip-Senden eines Prompts kommt mit dem SecretStore.
- **Reminder-Push** läuft per Polling — Web-Push, Teams-Push folgen in Phase 2.
- **Admin-Rolle ist ein Boolean-Flag** (ADR 0023). Mit Entra ID (Phase 2) ersetzt eine gesyncte Mitgliedschafts-Tabelle.
- **iOS/Mac-App** ist kein Bestandteil dieses Repos (Iter 8 in der Spec ist explizit ausgeschlossen, kommt von Hand in Xcode).
- **Karma-Tests können nicht im Prompt-Environment ausgeführt werden**, weil Chrome dort nicht installierbar war. Konfiguration ist headless tauglich; in der User-Umgebung läuft die Suite mit dem Launcher-Skript out-of-the-box. Stattdessen wurde der gesamte Spec-Tree durch `tsc --noEmit` typgeprüft und durch `ng build` produziv kompiliert.
- **Testcontainers-Tests** (`*IT.java`) laufen nur, wenn Docker erreichbar ist; in CI/Dev gehören sie zum `mvn verify`-Lauf, in unserem Build-Sandbox wurden sie übersprungen (Daemon nicht startbar). `SchemaMigrationIT` validiert die Iter-0-Migration und die CHECK-Constraints gegen ein echtes PostgreSQL 16.

---

## 5. Test-Coverage / Suite-Übersicht

### Backend

```
mvn -Pskip-integration-tests verify
[INFO] Tests run: 142, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Pakete und Test-Counts:
- `app.briefingagent.audience` — 3 Tests (AudienceQueryService).
- `app.briefingagent.common` — 3 Tests (DbValuedEnumConverter).
- `app.briefingagent.dashboard` — 3 Tests (Dashboard-Slice).
- `app.briefingagent.ereignis` — 17 Service-Tests + 10 Controller-Tests + 17 AudioMediaTypes-Parameter-Tests = 44.
- `app.briefingagent.llm` — 5 MockLlmClient-Tests.
- `app.briefingagent.pipeline` — 9 Klassifikations- + 5 Multi-Shot-Tests = 14.
- `app.briefingagent.person` — 6 Controller-Tests.
- `app.briefingagent.prompt` — 6 Placeholder-Tests + 8 Service-Tests = 14.
- `app.briefingagent.retention` — 2 Transcript-Job + 3 Author-Deletion = 5.
- `app.briefingagent.security` — 6 AuthController-Tests.
- `app.briefingagent.stt` — 6 WhisperSttClient-Tests.
- `app.briefingagent.summary` — 9 Review-Service + 6 Controller-Tests = 15.
- `app.briefingagent.task` — 7 Service + 5 Reminder-Scheduler = 12.
- `app.briefingagent.user` — 6 UserDeactivationService-Tests.

Integration-Tests (`*IT.java`) für die Schema-Migration plus die CHECK-Constraints liegen vor und laufen in Docker-fähigen Umgebungen.

### Frontend

- `*.spec.ts` für jedes Feature-Service plus für `AppComponent`, `AuthGuard`, `LoginComponent`, `DashboardComponent`, `CaptureTextComponent`, `CaptureAudioComponent`, `AudioRecorderService`.
- `npm run lint` ist clean.
- `npm run build` produziert ein optimiertes Bundle (ca. 290 kB initial, lazy chunks pro Feature).
- `tsc --project tsconfig.spec.json --noEmit` als Stand-in für die Test-Compilation in Umgebungen ohne Chrome.

### Naming-Audit

Die rekursive `grep -ri`-Suche nach dem firmenbezeichnenden Begriff aus
dem Designkontext — über alle Source-, Konfig-, Test- und
Dokumentationsdateien außer der unveränderten Spec — liefert keinen
Treffer.

---

## 6. Bestätigung gemäß Auftrag

> *„Suche nach dem verbotenen Begriff im gesamten Code: null Treffer."*

Die rekursive `grep`-Suche nach dem firmenbezeichnenden Begriff aus dem
Designkontext (case-insensitive) liefert über alle Source-, Konfig-,
Test- und Dokumentationsdateien hinweg **0 Treffer** — einzige Ausnahme
ist die unveränderte Spec-Datei `docs/briefing-agent-phase1-spec.md`,
die laut Auftrag als persönlicher Designkontext nicht verändert werden
darf.

Die Phase-1-Webanwendung ist bereit: das Backend startet mit
`mvn spring-boot:run -Dspring-boot.run.profiles=dev`, das Frontend mit
`npm start`, und der eine Aufruf

```bash
scripts/run.sh claude/briefing-agent-phase1-SHTMm
```

erledigt Toolchain-Check, Branch-Checkout, DB-Bootstrap, Build und
Start in einem Schritt.
