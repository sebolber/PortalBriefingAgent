# Briefing Agent – Iteration Plan (Phase 1, Web)

| Feld | Wert |
|------|------|
| Bezug | `docs/briefing-agent-phase1-spec.md` |
| Scope | Iter 0 … Iter 7 (Backend + Angular-Frontend). **Iter 8 (iOS/Mac) ist NICHT Teil dieses Auftrags.** |
| Walking-Skeleton-Strategie | Jede Iteration liefert ein lauffähiges Inkrement, frühe E2E-Funktionalität. |
| Branch | `claude/briefing-agent-phase1-SHTMm` (per Auftrag) |

Legende DoD: messbare Akzeptanzkriterien. Edge Cases werden in der jeweiligen Iter durch Tests abgedeckt.

---

## Querschnittsregeln (gelten für alle Iterationen)

- **Naming**: Im Code, in Konfig, in Tests, in Doku darf der firmenbezeichnende Begriff aus dem Designkontext (case-insensitive, in der Spec genannt) NIRGENDS auftauchen. Pro Iter Phase C: Naming-Audit gegen genau diesen Begriff per `grep`.
- **Sicherheit > Langlebigkeit > Performance > Nachhaltigkeit > Usability** (strikte Reihenfolge bei Konflikten).
- **TDD**: Tests werden NICHT geändert, um Code grün zu bekommen.
- **ADRs** unter `docs/adr/NNNN-kurztitel.md` für jede nicht-triviale Entscheidung.
- **Conventional Commits**, kleingranular. Abschluss-Commit pro Iter: `feat(iter-N): <kurzbeschreibung>` + Tag `iter-N-complete`.
- **Build-Tools**: Java 21, Maven, Spring Boot 3.3.x, PostgreSQL 16, Flyway, Testcontainers, WireMock; Angular 18 mit Signals.
- **Java-Package-Root**: `app.briefingagent`. **CSS-Variablen**: `--color-brand-*`. **DB-Name**: `briefing_agent`.

---

## Iter 0 – Walking Skeleton

**Ziel:** Backend + Frontend kompilieren und sprechen miteinander; einfacher Text-Input erzeugt eine Summary über einen Mock-LLM-Adapter; Read-Only-Dashboard zeigt die letzten Ereignisse.

### Tasks
- **Repo-Struktur**: `backend/` (Maven Multi-Module nicht nötig, ein Modul reicht), `frontend/` (Angular Workspace), `docs/adr/`.
- **Backend-Setup**:
  - `pom.xml`: Spring Boot 3.3.x Parent, Web, Security, Validation, Data JPA, Flyway, PostgreSQL-Driver, Actuator.
  - `application.yml` mit Profilen `dev`, `test`, `prod`. Profile `test` nutzt Testcontainers via `spring.test.database.replace=NONE` + `@DynamicPropertySource`.
  - Spring Security: `LocalAuthenticationProvider` (BCrypt cost 12), Session-basiert mit CSRF aktiv, REST-Endpoints für Login/Logout.
  - DB-Migrationen (Flyway) für Kerntabellen: `user_account`, `person`, `person_persona`, `persongroup`, `persongroup_member`, `topic`, `topic_member`, `ereignis`, `summary` (vereinfachter Stand für Iter 0; weitere Felder kommen in Iter 1–4).
  - `LlmProviderClient`-Interface + erste `HardcodedMockLlmClient`-Impl (Iter 0; echte HTTP-Impl folgt in Iter 2/5).
  - Endpoint `POST /api/ereignisse` (Text-Input) → speichert Ereignis → triggert synchron Mock-LLM → speichert eine Summary mit Audience „Self".
  - Endpoint `GET /api/dashboard/recent` → liefert die letzten 7 Tage Ereignisse + Summaries des eingeloggten Autors.
  - Health-/Readiness-Endpunkt via Actuator.
- **Frontend-Setup**:
  - `ng new` Workspace, Standalone-Components, Routing, Signals, ESLint, Karma/Jasmine.
  - `core/auth` (Login-Page, AuthService mit Session-Cookie, AuthGuard).
  - `core/api` (typed HttpClient-Wrapper, ApiError-Modell).
  - `features/dashboard` (Recent-Activity-Liste, Loading + Empty State).
  - `features/ereignis` (Text-Eingabe-Form: Textarea + Submit).
  - Light Theme + Dark Theme via CSS-Variablen, Primärfarbe `#006ec7` als `--color-brand-primary` definiert.
- **Tests**:
  - Backend: `UserAccountRepositoryIT` (Testcontainers), `LocalAuthenticationProviderTest`, `EreignisServiceTest` (Mock-LLM), Web-Layer-Test mit `@WebMvcTest` für `/api/ereignisse`, `/api/dashboard/recent`.
  - Frontend: AuthService-Unit-Test, DashboardComponent-Component-Test, EreignisFormComponent-Component-Test.
- **Doku**:
  - `README.md` im Repo-Root (Setup, lokale Tools, Build, Run).
  - ADRs: `0001-build-tool-maven.md`, `0002-spring-boot-version.md`, `0003-bcrypt-cost-12.md`, `0004-flyway-naming.md`, `0005-angular-standalone-signals.md`, `0006-css-variable-naming.md`, `0007-no-spring-ai.md`.
- **Start-Skript** `scripts/run.sh`:
  - Übergabeparameter: Git-Branch-Name.
  - Prüft Vorhandensein und ggf. Installation von: Java 21, Maven, Node.js 20+, npm, Docker (für PostgreSQL via Compose).
  - Checkt den angegebenen Branch aus (`git fetch && git checkout`).
  - Startet PostgreSQL via `docker compose up -d db`.
  - Baut Backend (`./mvnw clean package -DskipTests` plus optional `-Pwith-tests`) und Frontend (`npm ci && npm run build`).
  - Startet Backend (Spring Boot) und liefert das Angular-Bundle als statische Ressource aus, sodass nur ein Prozess läuft.
  - Robust: jeder Schritt mit klarem Fehlerausgang, `set -euo pipefail`, idempotent (lässt bestehende Tools unangetastet, installiert nur Fehlendes).
  - Dokumentiert in der README.

### Edge Cases (mit Tests abgedeckt)
- Leere Textarea (`""`) → 400 mit klarer Fehlermeldung.
- Text > Hard-Cap 10 000 Zeichen → 400.
- Nicht-eingeloggter Zugriff auf `/api/...` → 401, KEINE Stacktraces.
- BCrypt-Hash-Vergleich bei falschem Passwort → 401, keine Timing-Attack-Anfälligkeit (delegated to Spring Security default).
- DB-Connection weg → 503 mit aussagekräftiger Meldung, Actuator zeigt DOWN.

### Definition of Done
- `./mvnw clean verify` grün.
- `npm run build`, `npm run lint`, `npm run test -- --watch=false --browsers=ChromeHeadlessNoSandbox` grün.
- Manueller E2E: Login → Text eingeben → Speichern → Dashboard zeigt das Ereignis + Mock-Summary.
- README erklärt Setup in unter 10 Minuten reproduzierbar.
- Naming-Audit gegen den verbotenen Begriff liefert 0 Treffer.
- Tag `iter-0-complete` gesetzt.

### Abhängigkeiten
Keine – Initialer Schritt.

### Geschätzter Aufwand (Selbstkontrolle, nicht zur Beschleunigung)
Sehr groß: Skelett, Auth, JPA-Setup, erstes Migrations-Set, Angular-Bootstrap. Erwartet: ~30 % des Gesamtaufwands.

---

## Iter 1 – Audio-Capture + Whisper-Integration

**Ziel:** Web-Capture (MediaRecorder) sendet Audio ans Backend, Backend ruft Whisper (über WireMock im Test, später echter STT-Provider) und persistiert das Transkript als `Ereignis`. Audio wird nie auf Disk geschrieben.

### Tasks
- **Backend**:
  - `SttProviderClient`-Interface, erste HTTP-Implementierung via Spring `RestClient`, Konfiguration aus Properties (Iter 1) – Provider-Tabelle kommt erst in Iter 5.
  - Endpoint `POST /api/ereignisse/audio` (multipart): nimmt Audio entgegen (max ~50 MB), streamt an Whisper-API, verwirft Bytes nach Antwort. Soft-Warning bei 10 Min, Hard-Cap 15 Min (Duration via Header-Hint vom Client oder via Probing).
  - Erweiterung Ereignis-Modell: `source_type`, `transcript_source`, `language`, `duration_seconds`, `truncated_at_limit`.
  - Sicherheitsdetails: Content-Type-Whitelist (`audio/webm`, `audio/mp4`, `audio/ogg`, `audio/wav`), MIME-Sniffing-Check.
  - Asynchron-Strategie: synchron für Iter 1; echte Async-Pipeline folgt in Iter 2.
- **Frontend**:
  - `features/ereignis/capture-audio.component.ts` mit MediaRecorder, Timer-Anzeige, Soft-Warning-Banner ab 10 Min, automatischer Stopp bei 15 Min, Visual-Level-Meter optional.
  - Permission-Handling für Mikrofon, klare Fehlermeldungen bei verweigerter Permission.
- **Tests**:
  - Backend-Integration mit WireMock für Whisper-Endpoint: erfolgreicher Lauf, 5xx vom STT, Timeout, deutsches Transkript zurück.
  - Backend-Unit: MIME-Validierung, Größe > Hard-Cap, leeres File, ungültiger Content-Type.
  - Frontend: CaptureAudioComponent mit gemocktem MediaRecorder-Wrapper-Service.
- **Doku**:
  - ADR `0009-audio-no-disk-persistence.md`.
  - ADR `0010-audio-mime-whitelist.md`.
  - (Die RestClient/Spring-AI-Begründung steht bereits in ADR `0007-no-spring-ai.md` aus Iter 0.)

### Edge Cases (mit Tests)
- Mikrofon abgelehnt → User-Fehlermeldung, kein Crash.
- Aufnahme genau bei 10 Min → Soft-Warning sichtbar.
- Aufnahme bei 15 Min → automatischer Stopp, `truncated_at_limit=true`.
- Whisper-Endpoint nicht erreichbar → 502 mit ID des fehlgeschlagenen Ereignisses zur Wiedervorlage. Ereignis wird mit `transcript_text=NULL` und Status-Flag (review_status=pending) gespeichert? Entscheidung: in Iter 1 wird **kein** Ereignis gespeichert, wenn STT fehlschlägt – einfacher Mental Model.
- Leere Audio-Datei → 400.
- Audio mit englischem Speaker → `language='en'` gespeichert.

### Definition of Done
- Audio-Aufnahme im Browser → Transkript im Dashboard sichtbar (mit Mock-Whisper).
- Tests grün (Backend + Frontend), Coverage neuer Klassen ≥ 80 %.
- ADRs vorhanden, README aktualisiert.
- Naming-Audit ok.

### Abhängigkeiten
Iter 0 (Ereignis-Modell, Auth, Dashboard).

---

## Iter 2 – Vollständiges Domänenmodell + Multi-Shot Summary + LLM-Klassifikation

**Ziel:** Personengruppe, Thema, Persona-Verbindungen sind im Modell und in der API; eine Audience-Klassifikation entscheidet welche Audiences relevant sind; eine Summary pro Audience wird parallel generiert.

### Tasks
- **Backend**:
  - Flyway-Migrationen: `persongroup`, `persongroup_member`, `topic`, `topic_member`, `person_persona`, `summary`-Erweiterungen (audience_*-Felder mit CHECK-Constraint).
  - CRUD-APIs: `PersonController`, `PersonGroupController`, `TopicController`, `PersonPersonaController`.
  - `AudienceQueryService` – aggregiert für die KI alle Audiences eines Autors mit Persona.
  - `AudienceClassificationService` – ruft LLM auf, parst JSON-Schema, mappt auf Domain-Audiences. Robustes JSON-Parsing (Jackson + manuelle Validierung), Confidence-Mapping `low|medium|high`.
  - `SummaryGenerationService` – parallel via `@Async` + `ExecutorService`-Bean, eine Summary pro Audience.
  - Spring Events: `EreignisAcceptedEvent` → triggert Pipeline.
  - JSON-Schema-Validierung der LLM-Antwort (Pflichtfelder, Confidence-Enum, audience-Referenzen müssen existieren und dem Autor gehören).
- **Frontend**:
  - Features `persons`, `persongroups`, `topics` mit List + Detail + Edit-Formular.
  - Persona-Eingabe (Textarea, Markdown nicht im UI gerendert – Plain Text gemäß Spec 9.4 für Edit-Bereiche).
  - Dashboard-Update: Summary-Karten zeigen Audience + Confidence + Reasoning.
- **Tests**:
  - Backend: AudienceClassificationServiceTest mit WireMock-LLM (Happy Path, Confidence=low, leere Audience-Liste, malformed JSON, unbekannte Audience-IDs, gemischte Audience-Typen).
  - Backend: SummaryGenerationServiceTest – parallele Ausführung, Audience-Reihenfolge stabil im Output, Persona-Text im Prompt enthalten.
  - Backend: CHECK-Constraint via Integration-Test (mehrfache audience_*-Felder gleichzeitig → Konflikt).
  - Frontend: PersonForm validates required, PersonGroupForm validates member-list non-empty.
- **Doku**:
  - ADR `0011-three-audience-entities.md` (begründet warum drei Tabellen statt einer abstrahierten).
  - ADR `0012-pipeline-synchronous-with-fallback.md` (Phase-1: synchron + Fallback-Topic; Async folgt mit Provider-Refactor).
  - ADR `0013-llm-json-graceful-parsing.md`.

### Edge Cases
- LLM antwortet mit malformed JSON → Retry 1×, dann Pipeline-Fehler, Ereignis bleibt im review_status=pending.
- Klassifikation liefert leere Audience-Liste → kein Summary, Dashboard zeigt „Keine relevante Audience erkannt", Ereignis trotzdem gespeichert.
- Audience-ID gehört nicht dem aktuellen Autor → wird gefiltert, Confidence loggt Warnung.
- Sehr viele Audiences (z. B. 30) → Klassifikation-Prompt darf nicht über Modell-Kontext laufen → Limit auf z. B. 50, danach Pagination/Filter.
- Zwei Summaries gleichzeitig in DB schreiben → CHECK-Constraint hält.
- Audience wird während Pipeline gelöscht → optimistic check, Summary für gelöschte Audience entfällt.
- Persona-Text leer für eine Personengruppe → 400 beim Anlegen (Persona ist Pflicht laut Spec 4.2).

### Definition of Done
- Pipeline läuft asynchron, Dashboard zeigt mehrere Summaries pro Ereignis.
- Tests grün, Coverage ≥ 80 %.
- Naming-Audit ok.

### Abhängigkeiten
Iter 0, Iter 1.

---

## Iter 3 – Review-Workflow + Edit/Regenerate + Audit-Trail

**Ziel:** Transkript-Review (Audio-Quelle), Summary-Review mit Edit und Regenerate, vollständiger Audit-Trail in `edit_history`.

### Tasks
- **Backend**:
  - Erweiterung `Ereignis`: `review_status`-Übergänge `pending → reviewed → released`.
  - Endpoints:
    - `POST /api/ereignisse/{id}/transcript-review/start` (pausiert die Pipeline)
    - `PATCH /api/ereignisse/{id}/transcript` (Edit)
    - `POST /api/ereignisse/{id}/transcript-review/release` (startet Pipeline)
  - Endpoints für Summary:
    - `PATCH /api/summaries/{id}` (Edit, fügt Eintrag in `edit_history`)
    - `POST /api/summaries/{id}/regenerate` (mit optionalem Feedback → erneuter LLM-Call)
    - `POST /api/summaries/{id}/accept` (`accepted_at=NOW()`)
  - `EditHistoryEntry` – append-only JSONB, validiert Schema {timestamp, author_id, change_type, before, after, feedback?}.
  - Locking: Pessimistic-Lock auf Summary während Regenerate, damit parallele Edits keinen verlorenen Zustand erzeugen.
- **Frontend**:
  - Transkript-Review-Komponente mit 10-Sekunden-Countdown (gut sichtbar, „Jetzt prüfen"-Button stoppt Countdown).
  - Plain-Text-Editor (textarea) für Transkript-Edit, Diff-Highlight optional.
  - Summary-Review-Komponente: Markdown-Render im Lesemodus (z. B. via `marked` + sanitizer), Edit-Mode toggelt zu textarea.
  - Regenerate-Dialog mit Feedback-Textarea + „Neu generieren"-Button.
  - Audit-Trail-Ansicht (chronologische Liste der Edits + Regenerate-Vorgänge).
- **Tests**:
  - Backend: edit-history append-only, race-condition zwei gleichzeitige Edits (zweiter Edit muss optimistisch fehlschlagen).
  - Backend: regenerate vs. accept – accept nicht erlaubt während laufendem Regenerate.
  - Backend: transcript-review release startet Pipeline einmal, nicht mehrfach.
  - Frontend: Countdown läuft korrekt, „Jetzt prüfen" pausiert.
- **Doku**:
  - ADR `0014-edit-history-jsonb-append-only.md`.
  - ADR `0015-markdown-sanitization-frontend.md`.

### Edge Cases
- Autor verlässt Browser während Countdown → Pipeline läuft, kein Hänger.
- Regenerate mit leerem Feedback → erlaubt (Fall „nochmal").
- Edit speichert identischen Text → kein Eintrag in edit_history.
- Regenerate während Summary bereits `accepted` → 409.
- Markdown enthält `<script>` → wird sanitized.

### Definition of Done
- Vollständiger Review-Loop manuell testbar.
- Audit-Trail im UI sichtbar.
- Tests grün, Coverage ≥ 80 %.

### Abhängigkeiten
Iter 2.

---

## Iter 4 – Task-Extraktion + Lifecycle + Reminder

**Ziel:** KI extrahiert Task-Kandidaten aus dem Transkript; Autor reviewt, akzeptiert oder verwirft; Tasks haben Status-History und Reminder.

### Tasks
- **Backend**:
  - Flyway: `task`, `task_status_history`, `task_reminder`.
  - `TaskExtractionService` – LLM-Call mit JSON-Schema, mapping auf 4 Zuweisungstypen (`assigned_to_person_id`, `assigned_to_persongroup_id`, `assigned_to_topic_id`, `assigned_to_self`).
  - CHECK-Constraint `task_exactly_one_assignment` – Integration-Test.
  - `TaskService` mit Status-Übergängen `open → in_progress → done|dropped`, History-Eintrag bei jedem Wechsel.
  - REST: `GET/POST/PATCH/DELETE /api/tasks`, `POST /api/tasks/{id}/status`.
  - `TaskReminderScheduler` (Spring Scheduling) – täglich 06:00 UTC, sucht `due_date ≤ tomorrow`, schreibt `task_reminder` und triggert Push.
  - Push-Mechanik: In Phase 1 reicht ein In-App-Notification-Endpoint (`GET /api/notifications` für Polling). ADR begründet Verzicht auf Web-Push in Phase 1.
- **Frontend**:
  - Task-List + Task-Detail + Inline-Edit + Status-Wechsel-Button.
  - Task-Review im Summary-Review (Tasks-Tab pro Ereignis): bearbeiten oder verwerfen vor Freigabe.
  - Notification-Badge (Polling alle 60 s, exponentielles Backoff bei Fehler).
- **Tests**:
  - Backend: Reminder-Scheduler einmaliger Versand (kein Doppel-Trigger durch task_reminder-Tabelle).
  - Backend: Status-History komplett bei multi-step Übergängen.
  - Backend: Task ohne Assignment → 400.
  - Frontend: TaskList rendert, Filter nach Status.
- **Doku**:
  - ADR `0016-task-reminder-poll-vs-push.md`.
  - ADR `0017-task-status-history-design.md`.

### Edge Cases
- Task ohne due_date → kein Reminder.
- Task gestern fällig + kein Reminder gesendet (z. B. Job war down) → einmaliger Nachtrag, danach normal.
- Task `done` und due_date in Zukunft → kein Reminder.
- Reminder-Job läuft zweimal am Tag (Crash + Neustart) → Idempotenz über `task_reminder.reminder_type` + UNIQUE-Index `(task_id, reminder_type, DATE(reminded_at))`.
- Task assigned_to_self und Autor wird deaktiviert → automatisch `dropped` (Iter 6 implementiert die Deaktivierung, Iter 4 bereitet die Logik vor).
- LLM extrahiert Tasks ohne Assignment-Hinweis → fallback `assigned_to_self=true`.

### Definition of Done
- Task-Flow End-to-End sichtbar.
- Reminder-Job läuft, Tests grün.
- Naming-Audit ok.

### Abhängigkeiten
Iter 2 (Pipeline), Iter 3 (Review).

---

## Iter 5 – Provider-Config-UI + Prompt-Template-Editor

**Ziel:** LLM- und STT-Provider sowie Prompt-Templates sind in der DB konfigurierbar; UI ermöglicht Anlegen, Testen, Aktivieren; Prompt-Editor mit Pflicht-Platzhalter-Validierung und „Test mit Beispiel-Daten".

### Tasks
- **Backend**:
  - Flyway: `llm_provider`, `llm_provider_usage`, `stt_provider`, `prompt_template` + partielle Unique-Indexe.
  - Secret-Storage-Abstraktion: `SecretStore`-Interface, `EnvSecretStore`-Implementierung (Phase 1: Secrets aus Umgebungsvariablen referenziert via `api_key_secret_ref`).
  - REST: CRUD für LlmProvider, SttProvider, LlmProviderUsage, PromptTemplate.
  - `POST /api/llm-providers/{id}/test` und `POST /api/stt-providers/{id}/test` – führt Test-Call durch, persistiert Latenz + Ergebnis.
  - PromptTemplate-Versionierung: beim Update neue Version mit `active=true`, alte auf `active=false`, partial unique index hält durch.
  - PromptTemplate Placeholder-Validierung: Service prüft alle Pflicht-Platzhalter pro purpose.
  - Migration der bisher hardcoded LLM-Calls (Iter 0–4) auf provider-basierte Auflösung.
  - Default-Prompts pro Autor in Migration `R__seed_default_prompts.sql` + Service-Logik beim ersten Login.
- **Frontend**:
  - `features/config/llm-providers` – Liste, Edit, „Verbindung testen", Aktivierung pro purpose.
  - `features/config/stt-providers` – analog, nur ein aktiver erlaubt.
  - `features/config/prompt-templates` – Liste pro purpose, Edit (Plain-Text-Textarea, Live-Anzeige Pflicht-Platzhalter), „Test mit Beispiel-Daten"-Button, „Vorherige Version wiederherstellen".
- **Tests**:
  - Backend: Partial-Unique-Index hält (zwei aktive Provider pro purpose → DB-Fehler, Service liefert 409).
  - Backend: PromptTemplate ohne Pflicht-Platzhalter → 400 mit klarer Liste fehlender Platzhalter.
  - Backend: Test-Call gegen WireMock-Endpoint → Latenz im Result.
  - Frontend: Aktivierung-Toggle, Validierung der Pflichtfelder.
- **Doku**:
  - ADR `0018-secret-storage-abstraction.md`.
  - ADR `0019-prompt-template-versioning.md`.
  - ADR `0020-default-prompt-seed.md`.

### Edge Cases
- Aktiver Provider wird gelöscht → 409 mit „Erst Aktivierung übertragen oder deaktivieren".
- Test-Call timeout (z. B. 30 s) → result=failed mit Timeout-Message.
- Prompt-Template gespeichert, gleichzeitig zweiter Tab aktiviert → optimistic locking, zweiter Save schlägt fehl.
- Default-Prompts werden für bereits existierende Autoren retro-aktiv angelegt (Repeatable-Migration + Service-Hook beim Login).
- Prompt-Test mit Beispiel-Daten ohne Verbindung zu LLM → klare Fehlermeldung.
- Vorherige Version wiederherstellen, wenn aktuelle Version nicht abgespeichert (Dirty Form) → Bestätigungs-Dialog.

### Definition of Done
- Konfigurations-UI vollständig nutzbar, Provider-Wechsel ohne Re-Deploy möglich.
- Tests grün, Coverage ≥ 80 %.
- ADRs aktualisiert.

### Abhängigkeiten
Iter 0–4 (alle vorigen Pipeline-Aufrufe werden refactored).

---

## Iter 6 – Retention-Jobs + Tombstone + Author-Deaktivierung

**Ziel:** DSGVO-Konzept lebt: Transkripte werden nach Frist genullt, Summaries gemäß Retention gelöscht, Personen können tombstoned werden, Autoren-Lifecycle vollständig.

### Tasks
- **Backend**:
  - Flyway: `person.deleted_at`, `person.pseudonym`, `user_account.status`, `deactivated_at`, `deletion_scheduled_at` (falls noch nicht aus Iter 0 vorhanden, sonst Defaults setzen).
  - Spring Scheduling-Beans:
    - `TranscriptRetentionJob` – nightly 02:00 UTC – setzt `transcript_text=NULL` bei Ereignissen > globaler Retention-Schwelle.
    - `SummaryRetentionJob` – nightly 02:15 UTC – löscht Summaries gemäß `audience.summary_retention_months` (für Personen ohne explizite Retention: Default 12 Monate; für `person` ohne Persongruppen-/Topic-Kontext: Default 12).
    - `AuthorDeletionJob` – nightly 03:00 UTC – Cascade-Delete für `deletion_scheduled_at < NOW()`.
  - `PersonTombstoneService` – `person.deleted_at=NOW()`, `pseudonym='Gelöschte Person #N'` (laufende Nummer aus Sequence), UI zeigt Pseudonym.
  - `UserDeactivationService` – setzt `status='inactive'`, `deactivated_at`, `deletion_scheduled_at=+6 Monate`, läuft assigned_to_self-Tasks auf `dropped` mit Status-History-Note.
  - Admin-Endpoints (Rolle `ADMIN`, separater Endpoint-Pfad `/api/admin/...`) – aber im Phase-1-Scope minimal: nur Deaktivieren von Usern und Tombstoning von Personen. Eigentlicher Admin-Mehrwert kommt in Phase 2.
- **Frontend**:
  - Admin-Bereich (separater Route-Block, nur für ADMIN-Rolle): Liste Autoren mit Deaktivieren-Button, Liste Personen mit Tombstone-Button.
  - UI-Anzeige eines tombstoned Person-Eintrags: Klartext-Name verborgen, Pseudonym sichtbar, Persona-Felder grau.
- **Tests**:
  - Backend: Retention-Jobs idempotent (zweiter Lauf am gleichen Tag = no-op).
  - Backend: Tombstone hält referenzielle Integrität (kein FK-Violation).
  - Backend: AuthorDeletionJob testet Cascade auf Tasks, Ereignisse, Summaries, person_persona, persongroup, topic.
  - Backend: Deaktivierter Author kann sich nicht einloggen (401).
  - Frontend: Tombstoned Person sichtbar mit Pseudonym.
- **Doku**:
  - ADR `0021-retention-job-strategy.md`.
  - ADR `0022-tombstone-pseudonym-numbering.md`.
  - ADR `0023-admin-role-minimal-phase1.md`.

### Edge Cases
- Sehr viele zu löschende Datensätze in einer Nacht → Batch-Verarbeitung in Chunks von 1 000 mit COMMIT pro Chunk.
- Retention für Audience mit `summary_retention_months=NULL` → kein Löschen (unbegrenzt).
- Author wird reaktiviert bevor `deletion_scheduled_at` → status zurück auf active, scheduled-Feld auf NULL.
- Tombstone bricht ab (z. B. Constraint-Verletzung) → Transaction-Rollback, Person bleibt aktiv.
- Reminder-Job aus Iter 4 läuft parallel zu Retention-Job → keine Konflikte durch unterschiedliche Zeiten + Read/Write-Trennung.

### Definition of Done
- Jobs sichtbar im Actuator/Scheduling-Endpoint.
- Tests grün, inkl. Time-Travel via `Clock`-Bean (Spring `Clock` injizieren, Test-Clock setzen).
- Naming-Audit ok.

### Abhängigkeiten
Iter 0–5.

---

## Iter 7 – Volltext-Suche + Audience-Detail + Recent-Activity + Konsum-UX

**Ziel:** „Von Anna zur Summary in 2 Klicks" funktioniert. PostgreSQL Full-Text-Search über Persona-Beschreibungen, Transkripte, Summaries.

### Tasks
- **Backend**:
  - Flyway: `tsvector`-Spalten + GIN-Indexe (`person`, `persongroup`, `topic`, `ereignis`, `summary`).
  - Generierte Spalten: `search_vector GENERATED ALWAYS AS (...) STORED` mit `to_tsvector('german', ...)`.
  - `SearchService` – `GET /api/search?q=...` → strukturierte Ergebnisse pro Audience-Typ + Ereignisse + Summaries; Ranking via `ts_rank_cd`.
  - Audience-Detail-Endpoint: `GET /api/audiences/{type}/{id}` mit Persona, letzten Summaries, offenen Tasks.
- **Frontend**:
  - Globale Suche oben in der Top-Bar (Autofocus auf Dashboard-Open), Tasten-Shortcut „/", Debouncing 300 ms.
  - Suche-Result-Liste gruppiert nach Audience-Typ + Ereignis-Treffer.
  - Audience-Detail-Seite mit Sticky-Header (Persona) + Summaries chronologisch + offene Tasks.
  - Recent-Activity-Dashboard: letzte 7 Tage + Overdue-Tasks oben.
  - Tastatur-Navigation: Pfeil-Tasten in Suchergebnissen, Enter öffnet, Esc schließt.
- **Tests**:
  - Backend: Such-Tests mit deutschen Stop-Words, Code-Switching „Anna meeting", Phrase-Suche, Sonderzeichen.
  - Backend: Ranking-Test – Persona-Hit höher gewichtet als Transcript-Hit.
  - Frontend: Suche-Debounce, Empty-State, Keyboard-Navigation.
- **Doku**:
  - ADR `0024-postgres-fts-german.md`.
  - ADR `0025-search-ranking-weights.md`.
  - ADR `0026-keyboard-shortcuts.md`.

### Edge Cases
- Suche mit Sonderzeichen `Müller` / `Mueller` → matching via Unaccent-Extension.
- Leere Suche → Top-Recent-Liste statt Treffer.
- Tombstone-Person ist im Suchresultat → erscheint mit Pseudonym.
- Sehr lange Suche (z. B. 500 Zeichen) → Limit + Fehlermeldung.
- Suche während Pipeline läuft → bisherige Daten zeigen, neue Summaries kommen nach pipeline-Abschluss in Auflösung.
- Audience-Detail einer gelöschten Audience → 404 mit klarer Meldung.

### Definition of Done
- „Anna" tippen → 2 Klicks bis Briefing.
- Tests grün, Coverage ≥ 80 %.
- Naming-Audit ok.
- Phase-1-Completion-Report `docs/phase-1-completion-report.md` erstellt.

### Abhängigkeiten
Iter 0–6.

---

## Übergreifende Definition of Done (Phase 1)

- Alle Iterationen 0–7 abgeschlossen, jeweils mit Tag.
- `./mvnw clean verify` grün (Backend).
- `npm run build && npm run lint && npm test -- --watch=false` grün (Frontend).
- Die rekursive `grep`-Suche nach dem firmenbezeichnenden Begriff aus dem Designkontext liefert über alle Source-, Konfig-, Test- und Dokumentationsdateien hinweg **genau 0 Treffer** (außer in `docs/briefing-agent-phase1-spec.md`, das laut Auftrag unberührt bleibt).
- README im Repo-Root setzt Setup, Build, Run, Test-Suite reproduzierbar in unter 10 Minuten.
- `docs/phase-1-completion-report.md` listet umgesetzte Features, ADRs, Edge-Cases, offene Phase-2-Punkte.

---

## Iterations-Status

| Iter | Status | Datum |
|------|--------|-------|
| 0 | erledigt | 2026-05-12 |
| 1 | erledigt | 2026-05-12 |
| 2 | erledigt | 2026-05-12 |
| 3 | erledigt | 2026-05-12 |
| 4 | pending | – |
| 5 | pending | – |
| 6 | pending | – |
| 7 | pending | – |
