# Voice Briefing Tool – Phase-1-Spezifikation

| Feld | Wert |
|------|------|
| **Projekt** | Voice Briefing Tool (Arbeitstitel) |
| **Version** | 1.0 |
| **Datum** | 2026-05-12 |
| **Status** | Designphase abgeschlossen, bereit zur Implementierung |
| **Autor** | Sebastian Olbert |
| **Kontext** | adesso health solutions, offiziell sanktioniert |

---

## Inhaltsverzeichnis

1. [Überblick](#1-überblick)
2. [Mentales Modell](#2-mentales-modell)
3. [Architektur](#3-architektur)
4. [Domänenmodell](#4-domänenmodell)
5. [Datenmodell](#5-datenmodell)
6. [Workflows](#6-workflows)
7. [KI-Integration](#7-ki-integration)
7a. [Prompt-Templates](#7a-prompt-templates)
8. [Sicherheit & Compliance](#8-sicherheit--compliance)
9. [Frontend](#9-frontend)
10. [Phase-1 vs. Phase-2-Scope](#10-phase-1-vs-phase-2-scope)
11. [Implementierungs-Roadmap](#11-implementierungs-roadmap)
12. [Offene juristische Klärungen](#12-offene-juristische-klärungen)
13. [Test-Datenkonzept](#13-test-datenkonzept)
14. [Glossar](#14-glossar)
- [Anhang A: Entscheidungs-Log](#anhang-a-entscheidungs-log)

---

## 1. Überblick

### 1.1 Zweck

Das Voice Briefing Tool ist ein persönliches Wissens- und Briefing-Werkzeug, mit dem ein Autor (Berater, Lead Developer, Vertriebsmitarbeiter) erlebte Termine, Workshops oder Vertriebsereignisse als Sprachnachricht oder Textnotiz erfasst und durch eine KI in zielgruppengerechte Zusammenfassungen sowie konkrete Aufgaben umsetzen lässt. Vor dem nächsten Termin mit einer betreffenden Person, Personengruppe oder zu einem Thema kann der Autor das Briefing abrufen und manuell weiterverteilen.

### 1.2 Zielnutzer

- 2–10 Autoren bei adesso health solutions, primär in beratenden, vertrieblichen oder technisch-leitenden Rollen
- Jeder Autor pflegt seine eigene Wissensbasis, geteilter Personenstamm im Team
- Empfänger der Briefings sind in Phase 1 **keine** Nutzer des Tools – sie werden lediglich als Persona modelliert

### 1.3 Kontext und Compliance

- Officially sanctioned bei adesso health solutions, IT-Sicherheitsfreigabe vor produktivem Go-Live erforderlich
- Hybrid-Hosting: KI-Layer on-premises (Whisper + LLM auf vorhandener GPU-Infrastruktur), drumherum adesso-Cloud-Services für Auth, Logging, Backup
- Phase 1 ausschließlich mit fiktiven Test-Daten betrieben (lokales Auth, keine echten Sales-Inhalte)
- Datenhoheit verbleibt im adesso-Rechenzentrum

### 1.4 Phase-Konzept

| Phase | Inhalt | Auth | Daten |
|-------|--------|------|-------|
| **Phase 1** | Funktionale Reife, Test-Daten | Lokales Auth (Username/Passwort) | Fiktiv, keine echten Sales-Inhalte |
| **Phase 2** | Produktivbetrieb | Entra ID SSO + Personen-Sync + Group-Sync | Echte Daten, mit DSB-Freigabe |

Migrations-Trigger von Phase 1 auf Phase 2: **Feature-Reife** (Aufnahme, Klassifikation, Summary, Aufgaben laufen zuverlässig in Test-Daten).

### 1.5 Validierte Kern-Use-Cases

Vier Situationen wurden gegen die Architektur geprüft und sind unterstützt:

1. **Strategie-Workshop**: Autor pastet Protokoll als Text, Tool generiert Summaries für Führungskraft + alle Mitarbeiter und extrahiert Aufgaben
2. **Audio nach Kundenmeeting**: Autor spricht im Auto eine Notiz ein, Tool transkribiert via Whisper und adressiert das relevante Produkt-Thema
3. **Kurze Info am Mac**: Autor tippt 2 Sätze über ein verschicktes Angebot, Tool erkennt Deal-Größe und adressiert Vorstand sowie Produkt-Thema
4. **10-Min Reflexion über Dialog**: Autor spricht nach einem Gespräch 10 Minuten monologisch über das Erlebte, Tool generiert Summary für definierten Personenkreis

Multi-Speaker-Live-Aufnahmen sind bewusst **nicht** im Scope – siehe Abschnitt 10.

---

## 2. Mentales Modell

Das Tool ist ein **Personal Briefing Tool**, kein Distribution-System.

| Aspekt | Phase 1 |
|--------|---------|
| **Inhaber der Daten** | Der Autor |
| **Konsument der Summaries** | Der Autor selbst |
| **Verteilung an Empfänger** | Manuell, durch den Autor beim Termin |
| **Empfänger-Sichtbarkeit** | Keine – Empfänger haben keinen App-Zugang, sind nur als Persona modelliert |
| **Auto-Verteilung** | Phase 2 (E-Mail, Teams-Kanal), wenn Vertrauen ins Tool besteht |

**Konsequenz**: Vor einem Termin öffnet der Autor das Tool, ruft das Briefing zu einer Person, Personengruppe oder einem Thema ab, liest es vor, teilt Auszüge in einem Chat oder pflegt sie ins Protokoll ein. Das Tool selbst kommuniziert nicht nach außen.

---

## 3. Architektur

### 3.1 Top-Level-Komponenten

```
┌──────────────────┐    ┌─────────────────────────────────────┐
│  Angular Web-    │    │  iOS/Mac App (SwiftUI Multiplatform)│
│  Dashboard       │    │  - Capture                          │
│  - Verwaltung    │    │  - Schnellzugriff Briefing          │
│  - Konsum        │    └────────────────────┬────────────────┘
│  - Konfiguration │                         │
└────────┬─────────┘                         │
         │                                   │
         └────────────────┬──────────────────┘
                          │ HTTPS / REST
                          ▼
                ┌─────────────────────┐
                │  Spring Boot 3.x    │
                │  Backend (Java 21)  │
                │  - REST API         │
                │  - Auth (Phase 1:   │
                │    lokal, Phase 2:  │
                │    OAuth2/Entra)    │
                │  - Pipeline-Orchest.│
                └──┬──────────────┬───┘
                   │              │
                   ▼              ▼
        ┌──────────────────┐ ┌─────────────────┐
        │  PostgreSQL 16   │ │  KI-Services    │
        │  - Datenmodell   │ │  (on-prem GPU)  │
        │  - Volltext-     │ │                 │
        │    suche         │ │  - Whisper      │
        │  - Tombstones    │ │  - LLM (Llama   │
        │                  │ │    3.3 70B o.ä.)│
        └──────────────────┘ └─────────────────┘
```

### 3.2 Hosting-Modell

| Schicht | Ort | Begründung |
|---------|-----|------------|
| Audio-Verarbeitung (Whisper) | On-prem GPU bei adesso health solutions | DSGVO, Datenhoheit |
| LLM-Inferenz | On-prem GPU bei adesso health solutions | DSGVO, Datenhoheit |
| Persistenz (PostgreSQL) | adesso-RZ | DSGVO |
| Backend-Anwendung (Spring Boot) | adesso-RZ | Standardpfad |
| Frontend-Hosting (Angular) | adesso-RZ | Standardpfad |
| Auth-Layer | Phase 1 lokal, Phase 2 Entra ID Cloud | Stufenweise |
| Backup, Monitoring, Logging | adesso-Cloud-Services | Bestehende Konventionen |

### 3.3 Backend-Stack

- **Java 21**, **Spring Boot 3.x**
- **PostgreSQL 16** + **Flyway** (DDL-Konvention `docs/YYYYMMDD/`)
- **Spring Data JPA** / Hibernate
- **Spring Security** mit abstrahiertem `AuthenticationProvider` – Phase 1: `LocalAuthenticationProvider`, Phase 2 zusätzlich `OAuth2AuthenticationProvider` für Entra OIDC
- **Spring Scheduling** für nächtliche Retention- und Reminder-Jobs
- **Spring Events + `@Async`** für asynchrone KI-Pipeline-Schritte (kein externer Message-Broker in Phase 1)
- **Spring RestClient** für OpenAI-kompatible LLM/STT-API-Calls (keine Spring AI Bindung, weil Provider-Konfiguration zur Laufzeit aus DB kommt)

### 3.4 Test-Stack

- **JUnit 5** + **AssertJ**
- **Testcontainers** für PostgreSQL-Integration-Tests
- **WireMock** für Mock-LLM-/STT-Server in der KI-Pipeline-Testung
- TDD-Konvention "Ändere NICHT die Tests" gilt analog zu PortalCore/CVM

---

## 4. Domänenmodell

### 4.1 Entitäten-Übersicht

```
                ┌──────────┐
                │   User   │  (Autor)
                └────┬─────┘
                     │
        ┌────────────┼─────────────────────────┐
        │            │                         │
        ▼            ▼                         ▼
┌──────────────┐ ┌──────────┐         ┌──────────────┐
│  Ereignis    │ │   Task   │         │ PromptTemplate│
│  (Recording) │ └──────────┘         │ (per Author)  │
└──────┬───────┘                      └──────────────┘
       │
       ▼
┌──────────────┐
│   Summary    │ (eine pro Audience)
└──────┬───────┘
       │
       ▼
┌──────────────────────────────────────────────────┐
│  Audience (eine von drei):                       │
│  - Person                                        │
│  - Personengruppe (mit Personen-Mitgliedern)    │
│  - Thema (mit Personen-Mitgliedern)              │
└──────────────────────────────────────────────────┘

           Provider-Konfiguration (global):
           ┌─────────────┐  ┌──────────────────┐
           │ LlmProvider │──│ LlmProviderUsage │
           └─────────────┘  └──────────────────┘
           ┌─────────────┐
           │ SttProvider │
           └─────────────┘
```

### 4.2 Drei getrennte Audience-Konzepte

Bewusste Entscheidung: getrennte Entitäten statt einer abstrahierten "Audience"-Tabelle.

| Entität | Zweck | Persona |
|---------|-------|---------|
| **Person** | Einzelperson (Führungskraft, einzelner Kontakt) | optional pro Person |
| **Personengruppe** | feste Liste von Personen mit gemeinsamer Persona ("Vorstand", "Mein Team") | verpflichtend |
| **Thema** | Sachgebiet mit zugeordneten Betreuer-Personen ("Produkt CVM") | verpflichtend |

Persona-Beschreibung ist der **Schlüssel zur zielgruppengerechten Summary**. Sie fließt als System-Prompt-Fragment in den LLM-Call ein.

### 4.3 Personen-Stamm: geteilt im Team

- `Person` ist **global** über alle Autoren geteilt: Name, E-Mail, Rolle, Firma, Quelle (`manual` | `entra`)
- `PersonPersona` ist die Verbindung Autor × Person mit der individuellen Persona-Beschreibung (jeder Autor hat eine eigene Sicht auf Anna Müller)
- `Personengruppe` und `Thema` sind **pro Autor** definiert
- Recordings, Summaries und Tasks sind **pro Autor** und nicht geteilt

### 4.4 Ereignis statt Recording

Bewusste Namenswahl: **Ereignis** als Oberbegriff, weil sowohl Audio-Aufnahmen als auch direkt eingegebene Texte erfasst werden. Das Konzept ist Input-Modus-neutral.

---

## 5. Datenmodell

### 5.1 Kerntabellen

```sql
-- Autor / Nutzer
CREATE TABLE user_account (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255),                    -- Phase 1: lokal
    entra_object_id VARCHAR(36),                   -- Phase 2: Entra
    entra_upn VARCHAR(255),                        -- Phase 2: Entra
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',  -- 'active' | 'inactive'
    deactivated_at TIMESTAMP,
    deletion_scheduled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Geteilter Personenstamm
CREATE TABLE person (
    id UUID PRIMARY KEY,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(255),
    role VARCHAR(200),
    company VARCHAR(200),
    source VARCHAR(20) NOT NULL DEFAULT 'manual',  -- 'manual' | 'entra'
    deleted_at TIMESTAMP,                          -- Tombstone-Pattern
    pseudonym VARCHAR(100),                        -- bei Tombstone: "Gelöschte Person #1234"
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Persona pro Autor × Person (optional)
CREATE TABLE person_persona (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL REFERENCES user_account(id),
    person_id UUID NOT NULL REFERENCES person(id),
    persona_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(author_id, person_id)
);

-- Personengruppe (pro Autor)
CREATE TABLE persongroup (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL REFERENCES user_account(id),
    name VARCHAR(200) NOT NULL,
    persona_text TEXT NOT NULL,
    summary_retention_months INT NOT NULL DEFAULT 12,  -- "unbegrenzt" = NULL
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE persongroup_member (
    persongroup_id UUID NOT NULL REFERENCES persongroup(id),
    person_id UUID NOT NULL REFERENCES person(id),
    PRIMARY KEY (persongroup_id, person_id)
);

-- Thema (pro Autor)
CREATE TABLE topic (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL REFERENCES user_account(id),
    name VARCHAR(200) NOT NULL,
    persona_text TEXT NOT NULL,
    summary_retention_months INT NOT NULL DEFAULT 12,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE topic_member (
    topic_id UUID NOT NULL REFERENCES topic(id),
    person_id UUID NOT NULL REFERENCES person(id),
    PRIMARY KEY (topic_id, person_id)
);

-- Ereignis (vormals Recording): Audio oder Text als Eingangsquelle
CREATE TABLE ereignis (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL REFERENCES user_account(id),
    source_type VARCHAR(20) NOT NULL,              -- 'audio' | 'text'
    transcript_text TEXT,                          -- bei Audio: aus Whisper, bei Text: direkt
    transcript_source VARCHAR(20),                 -- 'whisper' | 'manual'
    language VARCHAR(10),                          -- 'de' | 'en' | ... (von Whisper detektiert)
    duration_seconds INT,                          -- nur bei audio
    character_count INT,                           -- nur bei text
    truncated_at_limit BOOLEAN NOT NULL DEFAULT FALSE,
    review_status VARCHAR(20) NOT NULL DEFAULT 'pending',  -- 'pending' | 'reviewed' | 'released'
    transcript_retention_until TIMESTAMP,          -- für nachträgliche Lifecycle-Bereinigung
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Summary: eine pro adressierter Audience
CREATE TABLE summary (
    id UUID PRIMARY KEY,
    ereignis_id UUID NOT NULL REFERENCES ereignis(id),
    audience_type VARCHAR(20) NOT NULL,            -- 'person' | 'persongroup' | 'topic'
    audience_person_id UUID REFERENCES person(id),
    audience_persongroup_id UUID REFERENCES persongroup(id),
    audience_topic_id UUID REFERENCES topic(id),
    summary_text TEXT NOT NULL,                    -- Markdown
    classification_confidence VARCHAR(10),         -- 'low' | 'medium' | 'high'
    classification_reasoning TEXT,                 -- 1-Satz-Begründung des LLM
    edit_state VARCHAR(20) NOT NULL DEFAULT 'ai_generated',  
                                                   -- 'ai_generated' | 'manually_edited' | 'regenerated'
    edit_history JSONB,                            -- Liste von Edits mit Timestamp + Author
    llm_provider_id UUID NOT NULL REFERENCES llm_provider(id),
    prompt_template_id UUID NOT NULL REFERENCES prompt_template(id),
    accepted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT summary_one_audience_type CHECK (
        (CASE WHEN audience_person_id IS NOT NULL THEN 1 ELSE 0 END) +
        (CASE WHEN audience_persongroup_id IS NOT NULL THEN 1 ELSE 0 END) +
        (CASE WHEN audience_topic_id IS NOT NULL THEN 1 ELSE 0 END) = 1
    )
);

-- Task: aus Ereignis extrahiert
CREATE TABLE task (
    id UUID PRIMARY KEY,
    ereignis_id UUID REFERENCES ereignis(id),      -- nullable: kann unabhängig erstellt werden
    author_id UUID NOT NULL REFERENCES user_account(id),
    title VARCHAR(200) NOT NULL,
    description TEXT,                              -- Markdown
    assigned_to_person_id UUID REFERENCES person(id),
    assigned_to_persongroup_id UUID REFERENCES persongroup(id),
    assigned_to_topic_id UUID REFERENCES topic(id),
    assigned_to_self BOOLEAN NOT NULL DEFAULT FALSE,
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'open',    -- 'open' | 'in_progress' | 'done' | 'dropped'
    completed_at TIMESTAMP,
    dropped_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT task_exactly_one_assignment CHECK (
        (CASE WHEN assigned_to_person_id IS NOT NULL THEN 1 ELSE 0 END) +
        (CASE WHEN assigned_to_persongroup_id IS NOT NULL THEN 1 ELSE 0 END) +
        (CASE WHEN assigned_to_topic_id IS NOT NULL THEN 1 ELSE 0 END) +
        (CASE WHEN assigned_to_self THEN 1 ELSE 0 END) = 1
    )
);

CREATE TABLE task_status_history (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES task(id),
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    note TEXT,
    changed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    changed_by_author_id UUID NOT NULL REFERENCES user_account(id)
);

CREATE TABLE task_reminder (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES task(id),
    reminded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    reminder_type VARCHAR(30) NOT NULL             -- 'one_day_before' | 'on_due_date'
);
```

### 5.2 Provider-Konfiguration

```sql
-- LLM-Provider (z.B. Llama 3.3 70B auf lokaler GPU, Reserve-Qwen, ...)
CREATE TABLE llm_provider (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    endpoint_url VARCHAR(500) NOT NULL,
    model_name VARCHAR(200) NOT NULL,
    api_key_secret_ref VARCHAR(200),               -- Referenz auf Secret-Storage
    parameters JSONB,                              -- temperature, top_p, max_tokens, etc.
    api_type VARCHAR(50) NOT NULL DEFAULT 'openai_compatible',
    last_tested_at TIMESTAMP,
    last_test_result VARCHAR(20),                  -- 'success' | 'failed'
    last_test_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Junction: Verwendungszwecke pro Provider
CREATE TABLE llm_provider_usage (
    id UUID PRIMARY KEY,
    llm_provider_id UUID NOT NULL REFERENCES llm_provider(id),
    purpose VARCHAR(50) NOT NULL,                  -- 'audience_classification' | 'summary_generation' | 'task_extraction' | 'transcript_correction'
    active BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (llm_provider_id, purpose)
);

-- Pro Zweck genau ein aktiver Provider
CREATE UNIQUE INDEX llm_provider_usage_one_active_per_purpose
    ON llm_provider_usage (purpose) WHERE active = TRUE;

-- STT-Provider (Whisper-Service oder Alternative)
CREATE TABLE stt_provider (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    endpoint_url VARCHAR(500) NOT NULL,
    model_name VARCHAR(200) NOT NULL,
    api_key_secret_ref VARCHAR(200),
    parameters JSONB,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    last_tested_at TIMESTAMP,
    last_test_result VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX stt_provider_one_active
    ON stt_provider (active) WHERE active = TRUE;
```

### 5.3 Prompt-Templates

```sql
-- Prompt-Templates pro Autor (Personal Briefing Tool: jeder hat eigene)
CREATE TABLE prompt_template (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL REFERENCES user_account(id),
    purpose VARCHAR(50) NOT NULL,                  -- gleiche Werte wie llm_provider_usage.purpose
    content TEXT NOT NULL,                         -- Prompt mit Platzhaltern wie {{transcript}}
    version INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by_author_id UUID NOT NULL REFERENCES user_account(id)
);

-- Pro Autor und Zweck genau ein aktives Template
CREATE UNIQUE INDEX prompt_template_one_active_per_author_purpose
    ON prompt_template (author_id, purpose) WHERE active = TRUE;
```

### 5.4 Kritische Constraints – Zusammenfassung

| Constraint | Zweck |
|------------|-------|
| `summary_one_audience_type` (CHECK) | Eine Summary hat genau eine Audience-Referenz |
| `task_exactly_one_assignment` (CHECK) | Ein Task ist genau einer Empfänger-Kategorie zugewiesen |
| `llm_provider_usage_one_active_per_purpose` (Partial Unique) | Pro Verwendungszweck nur ein aktiver LLM-Provider |
| `stt_provider_one_active` (Partial Unique) | Nur ein aktiver STT-Provider |
| `prompt_template_one_active_per_author_purpose` (Partial Unique) | Pro Autor und Zweck nur ein aktives Prompt |

---

## 6. Workflows

### 6.1 Capture-Workflow

**Audio-Variante:**

1. Autor öffnet App (Web oder iOS/Mac) und startet eine neue Aufnahme
2. Audio wird per Multipart-HTTP-POST ans Backend gesendet (Soft-Warning bei 10 Min, Hard-Cap bei 15 Min)
3. Backend empfängt Audio in Memory-Buffer oder kurzlebigem Tempfile
4. Backend leitet Audio an aktiven STT-Provider weiter (Whisper-Service)
5. Whisper transkribiert (deutsch primär, englisch sekundär)
6. **Audio wird verworfen** – nie auf persistente Disk geschrieben
7. Transkript wird in `Ereignis.transcript_text` persistiert mit `source_type='audio'`

**Text-Variante:**

1. Autor öffnet "Neues Ereignis"-Dialog in Web/Mac
2. Tippt oder pastet Text (Hard-Cap 10.000 Zeichen)
3. Text wird direkt als `Ereignis` mit `source_type='text'`, `transcript_source='manual'` gespeichert
4. Kein Transkript-Review-Schritt (Autor hat selbst geschrieben)

### 6.2 KI-Pipeline

Nach Speicherung des Ereignisses (bzw. nach Transkript-Review bei Audio-Quelle):

```
Ereignis (Transkript)
   │
   ▼
[1] audience_classification (LLM-Call)
   - Eingabe: Transkript + alle Audiences des Autors mit Personas
   - Ausgabe: JSON mit relevanten Audiences + Confidence + Begründung
   │
   ├──────────────────────┬──────────────────────┐
   ▼                      ▼                      ▼
[2a] summary_generation  [2b] summary_generation  ... (parallel)
     für Audience A           für Audience B
     ─────────                ─────────
     Eine Summary pro Audience, mit jeweiliger Persona im Prompt
   │
   ▼
[3] task_extraction (LLM-Call)
    - Eingabe: Transkript + Autor-Kontext
    - Ausgabe: JSON mit Task-Kandidaten (title, description, due_date-Vorschlag, Zuweisung)
   │
   ▼
Push-Notification an Autor: "Bereit zum Review"
```

### 6.3 Review-Workflow

**Transkript-Review (nur bei Audio-Quelle):**

- Autor sieht Transkript nach Whisper-Lauf
- Standardverhalten: 10-Sekunden-Counter, KI-Pipeline startet automatisch
- Optional: "Jetzt prüfen" → Pipeline pausiert, Transkript editierbar (Plain-Text-Korrektur), "Weiter" → Pipeline startet mit korrigierter Version
- Audio steht **nicht** zum Nachhören zur Verfügung (DSGVO-Datensparsamkeit)

**Summary-Review (immer):**

- Autor sieht pro adressierter Audience:
  - Vorgeschlagene Audience mit Confidence und LLM-Begründung – zu-/abwählbar
  - Summary-Text (Markdown-Rendering)
  - Edit-Button (Plain-Text-Editor, Edits werden in `edit_history` protokolliert)
  - "Mit Feedback neu generieren"-Button (Textfeld für Feedback → neuer LLM-Call mit angereichertem Prompt)
- Audit-Trail: Edits und Regenerationen werden mit Zeitstempel und Autor festgehalten
- Aufgaben werden parallel angezeigt: Titel, Beschreibung, Zuweisung, Fälligkeit – editierbar oder verwerfbar

**Freigabe:**

- Klick auf "Freigeben" → `Ereignis.review_status='released'`, `Summary.accepted_at=NOW()`
- Aufgaben werden in `Task`-Tabelle persistiert
- Damit ist das Briefing-Material in der App verfügbar

### 6.4 Briefing-Abruf (Konsum)

Vor einem Termin öffnet der Autor das Web-Dashboard oder die iOS/Mac-App:

| Einstieg | Aktion |
|----------|--------|
| Globale Suche | "Anna" oder "Vorstand" oder "Produkt CVM" oder Volltext-Stichwort tippen → Audience-Detail |
| Linke Navigation | Personen / Personengruppen / Themen / Tasks / Recordings |
| Recent Activity | Default-Ansicht im Dashboard: letzte 7 Tage + Overdue Tasks |
| Audience-Detail | Persona-Beschreibung als Sticky-Header + Summaries chronologisch + Tasks zur Audience |

**Mobile (iOS):** Search-First, Suchfeld direkt fokussiert beim App-Start. Quick-Access zu zuletzt geöffneten Audiences.

**Suche:** PostgreSQL Full-Text-Search (`tsvector`/`tsquery`) über Persona-Beschreibungen, Transkripte und Summaries. Kein separater Suchindex in Phase 1.

### 6.5 Aufgaben-Lifecycle

- Aufgaben entstehen aus der KI-Pipeline oder werden manuell erstellt
- Zustände: `open` → `in_progress` → `done` oder `dropped`
- Jeder Statuswechsel wird in `task_status_history` protokolliert mit optionaler Notiz
- Push-Erinnerung an Autor: 1 Tag vor `due_date` und am Tag selbst (`task_reminder` verhindert Mehrfachversand)
- Aufgaben mit `assigned_to_self=true` werden bei Autor-Deaktivierung automatisch auf `dropped` gesetzt
- Verteilung an Empfänger erfolgt in Phase 1 **nicht** automatisch – der Autor kommuniziert manuell beim Termin

### 6.6 Lifecycle-Jobs (Spring Scheduling)

| Job | Frequenz | Aufgabe |
|-----|----------|---------|
| Reminder-Job | täglich | Suche nach `Task` mit `due_date ≤ tomorrow` und Status `open|in_progress` → Push-Notification |
| Transkript-Retention | nächtlich | Setze `Ereignis.transcript_text` auf NULL für Ereignisse älter als globale Retention (Default 12 Monate) |
| Summary-Retention | nächtlich | Lösche Summaries gemäß `Audience.summary_retention_months` |
| Author-Deletion | nächtlich | Cascade-Delete für User mit `deletion_scheduled_at < NOW()` |

---

## 7. KI-Integration

### 7.1 Provider-Abstraktion

Alle KI-Aufrufe gehen über konfigurierbare Provider, nicht hartcodiert. Konsequenzen:

- Neuer Provider wird im UI angelegt mit Endpoint, Modell, API-Key (als Secret-Storage-Referenz), Parametern
- "Verbindung testen"-Button schickt Test-Request, zeigt Latenz + Modellantwort
- Aktivierung pro Verwendungszweck explizit per Klick – keine Automatik
- Ein Provider kann mehrere Verwendungszwecke gleichzeitig abdecken (z.B. ein einziges Llama-Modell für Klassifikation, Summary und Aufgaben)
- API-Keys werden nicht im Klartext in der DB gespeichert, sondern als Secret-Storage-Referenz (Vault, Azure Key Vault, Spring Cloud Vault – nach adesso-Standard)

### 7.2 OpenAI-kompatible API

Alle Provider sprechen die OpenAI-kompatible Chat-Completions-API. Standardmäßig unterstützte Serving-Stacks:

- **vLLM** (lokal auf GPU)
- **Ollama** (lokal)
- **Hugging Face TGI** (lokal)
- **NVIDIA NIM** (lokal)
- Optional in Phase 2: Cloud-Provider mit AVV (Azure OpenAI, Anthropic via Bedrock)

Spring RestClient + dünne Provider-Adapter-Schicht: ein Provider-Eintrag aus der DB → RestClient-Instanz → API-Call. Kein Spring AI Framework (Provider-spezifische Annahmen kollidieren mit dem dynamischen Konfigurationsmodell).

### 7.3 Empfohlene Modelle für Phase 1

| Zweck | Empfehlung | VRAM | Notiz |
|-------|------------|------|-------|
| Speech-to-Text | Whisper-large-v3 | ~10 GB | Multilingual, deutsch + englisch, Code-Switching |
| LLM (alle Zwecke) | Llama 3.3 70B Instruct (Q4_K_M) | ~40 GB | Starke deutsche Performance, zuverlässige JSON-Outputs |
| Alternative LLM | Qwen 2.5 72B Instruct (Q4_K_M) | ~42 GB | Noch besser bei Code-Switching, JSON robuster |

Quantisierung Q4_K_M reicht für die Qualitätsanforderungen. Lauffähig auf 1× H100 80GB oder 2× A100 40GB.

### 7.4 Verwendungszwecke

| Purpose | Eingabe | Ausgabe |
|---------|---------|---------|
| `audience_classification` | Transkript + alle Audiences mit Personas | JSON mit Liste relevanter Audiences + Confidence + Begründung |
| `summary_generation` | Transkript + eine Audience-Persona | Markdown-formatierte Summary für diese Audience |
| `task_extraction` | Transkript + Autor-Kontext | JSON mit Task-Kandidaten (title, description, suggested_due_date, suggested_assignee) |
| `transcript_correction` | Whisper-Rohtranskript + Fachbegriff-Kontext (optional) | Korrigiertes Transkript |

`transcript_correction` ist optional und gehört in Phase 1.5.

---

## 7a. Prompt-Templates

### 7a.1 Konzept

Jeder Verwendungszweck hat einen versionierten Prompt-Template, der vom Autor anpassbar ist. So kann er Tonalität, Länge, Fokus und Struktur der Summaries (und der Klassifikation, der Aufgaben-Extraktion) im Laufe der Zeit feinjustieren.

Da das Tool ein **Personal Briefing Tool** ist, sind Prompt-Templates pro Autor – Änderungen eines Autors beeinflussen nur seine eigenen Summaries.

### 7a.2 Versions-Mechanik

- Bei Bearbeitung eines Prompts entsteht ein neuer Datensatz mit `active=TRUE` und inkrementierter Version
- Der vorherige Datensatz wird auf `active=FALSE` gesetzt
- Versionsgeschichte = alle inaktiven Templates pro (Autor, Zweck)
- "Vorherige Version wiederherstellen": eine inaktive Version wird wieder aktiviert (die andere wandert ins Archiv)

### 7a.3 Platzhalter-System

Pro Zweck definierte Pflicht-Platzhalter:

| Zweck | Pflicht-Platzhalter |
|-------|---------------------|
| `audience_classification` | `{{transcript}}`, `{{audiences_with_personas}}` |
| `summary_generation` | `{{transcript}}`, `{{audience_name}}`, `{{audience_persona}}`, `{{language}}` |
| `task_extraction` | `{{transcript}}`, `{{author_name}}` |

Beim Speichern wird validiert, dass alle Pflicht-Platzhalter vorhanden sind, sonst Fehlermeldung.

### 7a.4 Audit-Trail

Jede generierte Summary speichert die `prompt_template_id` der zum Generierungszeitpunkt aktiven Version. Damit ist nachvollziehbar:

- Welcher Prompt wurde verwendet
- Wenn der Autor 3 Monate später feststellt, dass eine Summary nicht passt → Vergleich mit dem inzwischen geänderten Prompt
- A/B-Test verschiedener Prompts an demselben Transkript möglich

### 7a.5 UI-Konzept

- Liste aller Prompts (Tabelle: Zweck | Aktive Version | Bearbeitet am)
- Edit-Dialog: Plain-Text-Textarea, Live-Hinweis auf Pflicht-Platzhalter
- **"Test mit Beispiel-Daten"-Button**: führt Prompt gegen einen ausgewählten Beispiel-Datensatz aus, zeigt LLM-Output → Autor sieht Auswirkungen vor Aktivierung
- "Vorherige Version wiederherstellen"-Button neben jeder inaktiven Version

### 7a.6 Default-Prompts

Flyway-Migration legt initiale Default-Prompts pro Autor an (beim ersten Login: Trigger-basierte Initialisierung oder Service-Layer-Logik). Default-Texte werden in der Migration als Konstanten gepflegt.

---

## 8. Sicherheit & Compliance

### 8.1 Auth-Phasierung

**Phase 1 – Lokales Auth:**
- Username + Passwort, BCrypt-Hash
- Spring Security mit `LocalAuthenticationProvider`
- Nur Test-Daten zulässig, keine echten Sales-Inhalte
- Architektur ist Migration-fähig (Felder `entra_object_id`, `entra_upn` bereits in `user_account` vorgesehen)

**Phase 2 – Entra ID SSO + Personen-Sync:**
- OAuth2/OIDC-Flow gegen adesso-Entra-Tenant
- Spring Security mit zusätzlichem `OAuth2AuthenticationProvider`
- Personen-Stamm kann aus Entra-Verzeichnis befüllt werden (Microsoft Graph API)
- Externe Personen (Nicht-adesso) bleiben weiterhin manuell anlegbar (`person.source='manual'`)

**Phase 2.5 – Group-Sync (optional):**
- Personengruppen können aus Entra-Sicherheitsgruppen importiert werden
- Nur, wenn die Entra-Gruppen tatsächlich semantisch passen (in der Praxis oft nicht)

### 8.2 DSGVO-Konzept

| Datenart | Aufbewahrung | Mechanismus |
|----------|--------------|-------------|
| Audio | Wird nicht persistiert | Streaming/Tempfile, sofort verworfen nach Transkription |
| Transkript | Global konfigurierbar, Default 12 Monate | Nightly-Job nullt `transcript_text`, Tombstone-Hinweis |
| Summary | Konfigurierbar pro Audience (3–60 Monate oder unbegrenzt) | Nightly-Job löscht abgelaufene Summaries |
| Task | Dauerhaft, manuelle Löschung durch Autor möglich | Cascade-Delete bei Audience-Löschung |
| Personen-Stamm | Tombstone-Pattern | Auf Anforderung pseudonymisiert |
| Author-Daten | Bei Ausscheiden 6 Monate Sperrfrist, dann Cascade-Delete | Nightly-Job mit `deletion_scheduled_at`-Check |

### 8.3 Recht auf Vergessen

Wenn eine Person ihr Recht auf Löschung ausübt:

- `person.deleted_at = NOW()`, `person.pseudonym = "Gelöschte Person #N"`
- Alle Referenzen in `person_persona`, `persongroup_member`, `topic_member`, `task.assigned_to_person_id`, `summary.audience_person_id` bleiben technisch intakt
- UI zeigt die Person als "Gelöschte Person #N" an, ohne Klarnamen
- Transkripte und Summaries werden **nicht** rückwirkend bearbeitet – sie enthalten die eigenen Worte des Autors über eine andere Person, was juristisch separat zu bewerten ist (DSB-Klärung)

### 8.4 Author-Ausscheiden

- Beim Ausscheiden wird `user_account.status='inactive'`, `deactivated_at=NOW()`, `deletion_scheduled_at=NOW()+6 Monate` gesetzt
- Während der Sperrfrist: Zugang gesperrt, Daten bleiben für eventuelle Audit-/Compliance-Anfragen erhalten (Admin-Zugriff über separate Logik möglich)
- Nach Ablauf der Sperrfrist: Cascade-Delete aller Daten des Autors
- Aufgaben mit `assigned_to_self=true` und offenem Status werden bei Deaktivierung automatisch auf `dropped` gesetzt mit Status-History-Note "User deaktiviert"

### 8.5 Offene DSB-Klärungen

Vor produktivem Go-Live (Phase 2) mit dem adesso-Datenschutzbeauftragten zu klären:

1. **Rückwirkende Anonymisierung in Transkripten/Summaries** bei Recht-auf-Vergessen-Anforderung: Tombstone-only ausreichend, oder muss der Name in Transkripten/Summaries ersetzt werden?
2. **Aufbewahrungsfristen** für Transkripte und Summaries unter DSGVO-Zweckbindung
3. **Author-Ausscheiden**: 6 Monate Sperrfrist ausreichend? Welche Daten dürfen in dieser Zeit zugänglich bleiben?
4. **Externe Personen** (Kunden, andere Unternehmen): zusätzliche Consent-Anforderungen vor Aufnahme von Klarnamen in Briefing-Notizen?

---

## 9. Frontend

### 9.1 Angular-Web-Dashboard

- **Angular 18** (konsistent mit PortalCore/CVM)
- **Routing:** Recordings / Personen / Personengruppen / Themen / Tasks / Konfiguration
- **Globale Suche** als prominentes Eingabefeld oben
- **Default-Ansicht:** Recent Activity letzte 7 Tage + Overdue Tasks oben
- **Audience-Detail-Seiten** mit Sticky-Header für Persona-Beschreibung
- **Konfigurations-Bereich:** LLM-Provider, STT-Provider, Prompt-Templates, eigene Retention-Settings
- **Adesso Corporate Design** wird angewandt (konsistent mit Sebastian's CVM-Iteration 23): Primärfarbe `#006ec7`, Fira Sans, FontAwesome – Details siehe CVM-CD-Referenz

### 9.2 SwiftUI-Multiplatform (iOS + Mac)

- **Eine Codebase**, zwei Targets via SwiftUI Multiplatform
- **iOS:** Capture-First mit großem Mikrofon-Button, Search-First beim App-Start, Tab-Bar
- **Mac:** Konsum-Fokus mit Sidebar-Navigation, Search via Cmd+F, Mehrfenster-fähig
- **Apple-Watch-Integration** als Phase-2-Option (Capture per Watch-App)

### 9.3 Konsum-UX-Pattern

Kern-Anspruch: **"von Anna zur Summary in 2 Klicks"**

1. App auf
2. "Anna" tippen
3. → Person-Detail mit Persona, den 3 neuesten Summaries und offenen Tasks

Wenn das in mehr als 5 Sekunden dauert, wird das Tool im stressigen Moment vor einem Termin nicht genutzt.

### 9.4 Reviews und Edit-UIs

- **Transkript-Review:** Plain-Text-Editor, ohne Markdown-Formatierung
- **Summary-Review:** Markdown-Rendering im Lesemodus, beim Edit Plain-Text-Textarea (Edits speichern als Markdown)
- **Audio-Aufnahme:** klare Timer-Anzeige, Soft-Warning bei 10 Min, automatischer Stopp bei 15 Min
- **Provider/Prompt-Konfiguration:** Plain-Text-Editoren, kein WYSIWYG

---

## 10. Phase-1 vs. Phase-2-Scope

### 10.1 In Phase 1 enthalten

- Capture: Audio (Web + Mac, später iOS) und Text
- Whisper-Transkription mit Transkript-Review-Option
- Pure LLM-Audience-Klassifikation
- Multi-Shot Summary-Generierung
- Aufgaben-Extraktion und manuelles Tracking in der App
- Review-Workflow mit Edit + Regenerate
- Drei Audience-Typen (Person, Personengruppe, Thema) mit Personas
- Geteilter Personenstamm im Team
- LLM- und STT-Provider-Konfiguration mit Verwendungszwecken
- Prompt-Template-Konfiguration pro Autor mit Versionierung
- Lokales Auth (Username/Passwort)
- Retention-Lifecycle für Transkripte, Summaries, Personen-Tombstones, Author-Deaktivierung
- Web-Dashboard (Angular) + iOS/Mac-App (SwiftUI Multiplatform)
- Volltext-Suche via PostgreSQL

### 10.2 Bewusst NICHT in Phase 1

- Auto-Verteilung von Summaries (E-Mail, Teams-Kanal)
- Empfänger-Accounts und Empfänger-Dashboards
- Outlook Add-In
- Kalender-Integration / proaktive Briefing-Erinnerungen
- M365-ToDo-Spiegelung von Aufgaben
- Bidirektionale Sync-Mechanismen
- Multi-Speaker-Live-Aufnahmen
- Speaker Diarization
- Foto-Anhänge zu Aufnahmen
- Echte Sales-/Kundendaten

### 10.3 Phase-2-Aufbau

| Erweiterung | Migrations-Trigger |
|-------------|--------------------|
| Entra ID SSO + Personen-Sync | Feature-Reife in Phase 1 erreicht |
| Group-Sync aus Entra-Sicherheitsgruppen | Nach Phase-2-Pilotbetrieb, optional |
| Auto-Verteilung Summaries an Empfänger (E-Mail) | Vertrauen ins Tool gewachsen |
| Auto-Posting in Teams-Kanäle | Nach Auto-Mail-Stufe |
| M365-ToDo-Integration für Aufgaben | Bei stabiler Auto-Verteilung |
| Kalender-Integration für Briefing-Push | Nach Auto-Verteilungs-Reife |
| Vorbereitungs-Notizen mit Termin-Verknüpfung | Phase 1.5 |

---

## 11. Implementierungs-Roadmap

### 11.1 Walking-Skeleton-Ansatz

Frühe End-to-End-Funktionalität, jede Iteration liefert ein funktionierendes Inkrement. Backend-First würde lange ohne sichtbare Ergebnisse bleiben; UI-First mit Mocks würde KI-Latenz nicht realistisch testen.

### 11.2 Iterationen

| Iter | Inhalt | Wert nach Iteration |
|------|--------|---------------------|
| 0 | Backend-Setup, DB-Schema (Kerntabellen), Lokales Auth, Angular-Setup, ein LLM-Provider hartcodiert, Text-Input → eine Summary → Read-Only Dashboard | Erste E2E-Demo, alle Schichten zünden |
| 1 | Audio-Capture via Web (MediaRecorder), Whisper-Provider-Integration, Transkript-Persistierung | Audio-Flow läuft im Web |
| 2 | Vollständiges Domänenmodell (Personengruppe, Thema, Personas), Multi-Shot Summary, LLM-Audience-Klassifikation | Kern-Wertversprechen funktioniert |
| 3 | Review-Workflow: Transkript-Review + Summary-Review mit Edit/Regenerate, Audit-Trail | Autor sichert Qualität, vertraut Pipeline |
| 4 | Task-Extraktion, Task-Datenmodell, Statusverlauf, Reminder-Push | Aufgaben-Spur komplett |
| 5 | LLM-/STT-Provider-Konfig-UI, Junction-Tabelle, Prompt-Template-Editor | Provider- und Prompt-Wechsel ohne Re-Deploy |
| 6 | Retention-Jobs, Person-Tombstone, Author-Deaktivierung | DSGVO-Konzept lebt |
| 7 | PG-Volltext-Suche, Audience-Detail-UI, Recent Activity, Konsum-UX | Briefing-Abruf rund |
| 8 | iOS-App (SwiftUI Multiplatform), Mac-Target, Polishing, Test-Daten-Setup | Mobile + Demo-Ready |

### 11.3 Zeitabschätzung

Bei ~2 Wochen pro Iteration neben dem Tagesjob: **~16 Wochen / 4 Monate** für Phase 1. Parallel sollten ab Iter 6 die IT-Sicherheits-Freigabe und die DSB-Klärungen angestoßen werden, um den Phase-2-Übergang nicht zu blockieren.

---

## 12. Offene juristische Klärungen

Mit dem adesso-Datenschutzbeauftragten vor Phase-2-Go-Live abzustimmen:

1. **Tombstone-Pattern** für Person-Löschung: ausreichend, oder müssen Transkripte/Summaries rückwirkend bearbeitet werden?
2. **Aufbewahrungsfristen** für Summaries (default 12 Monate, konfigurierbar) im Lichte der Zweckbindung
3. **Author-Ausscheiden mit 6 Monaten Sperrfrist**: tragbar, oder kürzere Frist nötig?
4. **Recordings von Drittpersonen** (Kundennamen, Persona-Beschreibungen) ohne deren explizite Einwilligung: zulässige Verarbeitungsgrundlage?
5. **DSGVO-Folgenabschätzung (DSFA)** vermutlich erforderlich, weil regelmäßige systematische Verarbeitung personenbezogener Daten im großen Stil

---

## 13. Test-Datenkonzept

### 13.1 Personen- und Audience-Seed

- Flyway-Repeatable-Migration `R__seed_test_persons.sql` legt ~10 fiktive Personen an (Anna Müller, Bernd Schmidt, ...) mit konsistent fiktiven E-Mail-Adressen
- Audience-Seed legt 3 Personengruppen ("Vorstand-Fiktiv", "Mein Team-Fiktiv", "Alle Mitarbeiter-Fiktiv") und 3 Themen ("Produkt CVM-Test", "Cybersecurity-Test", "Strategie-Test") mit Persona-Beschreibungen an
- Geschäftsdaten in fiktiven Aufnahmen folgen der adesso-Health-Konvention: BBNR 12345678, VSNR A123456789

### 13.2 Reale Test-Aufnahmen

- Autor (Sebastian + ggf. Pilot-Kollegen) sprechen fiktive Szenarien selbst ein oder pasten fiktive Texte
- Audio-Aufnahmen testen Whisper realistisch (deutsche Aussprache, Code-Switching, Hintergrundgeräusche)
- Text-Inputs testen den Direkt-Pipeline-Pfad

### 13.3 Reproduzierbarkeit

- Falls reproduzierbare Drehbücher gewünscht: Markdown-Skripte unter `test-data/scripts/YYYYMMDD/` ablegen, die einsprechbar sind
- Bei LLM-Modell-Wechseln können dieselben Skripte erneut eingespielt und Outputs verglichen werden

### 13.4 Konsequenz für Phase 1

In der Test-Phase dürfen **keine echten Sales-Inhalte, Kundennamen, Sozialdaten oder Versichertendaten** in die App – das widerspräche dem lokalen Auth ohne IT-Sicherheitsfreigabe.

---

## 14. Glossar

| Begriff | Definition |
|---------|------------|
| **Autor** | Person, die das Tool nutzt, Ereignisse erfasst und Briefings konsumiert |
| **Audience** | Oberbegriff für Person, Personengruppe oder Thema – die Ziel-Persona, an die eine Summary gerichtet ist |
| **Ereignis** | Erfasste Wissens-Einheit (Audio-Aufnahme oder Text-Input), Quelle für die KI-Pipeline |
| **Persona** | Freitext-Beschreibung des Informationsbedürfnisses und Stils einer Audience, fließt als System-Prompt-Fragment in den LLM-Call ein |
| **Summary** | KI-generierte zielgruppengerechte Zusammenfassung eines Ereignisses für genau eine Audience |
| **Briefing** | Sicht auf alle Summaries und Tasks zu einer Audience (z.B. "Briefing für Anna" = alle Summaries zur Person Anna + offene Tasks zu Anna) |
| **Provider** | Konfigurierbarer KI-Service (LLM oder STT), angesprochen über OpenAI-kompatible API |
| **Verwendungszweck** | Funktionale Rolle eines LLM-Aufrufs: Klassifikation, Summary-Generierung, Aufgaben-Extraktion |
| **Prompt-Template** | Versionierter, pro Autor anpassbarer Prompt-Text mit Platzhaltern |
| **Tombstone** | Datensatz, der als gelöscht markiert ist, dessen Referenzen aber technisch intakt bleiben |

---

## Anhang A: Entscheidungs-Log

| # | Thema | Gewählt | Begründung (kurz) |
|---|-------|---------|-------------------|
| 1 | Kontext | Team von 2–10 (c) | Bewusste Multi-Autoren-Architektur von Anfang an |
| 2 | Approval | Officially sanctioned (a) | Compliance- und Sicherheits-konform |
| 3 | Plattform | Standalone (b) | Eigene Codebase, eigene IT-Standards |
| 4 | Hosting | Hybrid mit KI on-prem (b) | Datenhoheit + pragmatische Cloud-Services drumherum |
| 5 | GPU-Status | vorhanden (a) | Blocker eliminiert |
| 6 | Frontend | Angular-Web + SwiftUI Multiplatform | Web-Dashboard + iOS/Mac aus einer Codebase |
| 7 | Domänenmodell | drei getrennte Konzepte (b) | Person, Personengruppe, Thema explizit |
| 8 | Persona-Anker | alle drei Entitäten (b) | maximal flexibel |
| 9 | Multi-Audience-Summary | Multi-Shot pro Audience (a) | Persona-Treue maximal |
| 10 | Workflow nach Aufnahme | KI schlägt vor, Autor bestätigt (b) | Vertrauensschutz |
| 11 | Review-Aktionen | Edit + Regenerate (d) | beide Korrektur-Modi |
| 12 | Aufgaben-Verteilung | bleibt in App, manuelle Kommunikation (β) | konsistent mit Personal Briefing Tool |
| 13 | Briefing-Abruf | manueller Abruf (a) | Autor öffnet App aktiv beim Termin |
| 14 | Personal Briefing Tool | Modell bestätigt | Autor ist alleiniger Konsument |
| 15 | Author-Sicht | geteilter Personenstamm, sonst isoliert (b) | Stammdaten einmal pflegen |
| 16 | Capture-Modi | Audio (a) + Text-Input | Audio nach Termin + Text aus Clipboard |
| 17 | Transkript-Review | mit Pflicht-Review (b) | Fachbegriff-Kontrolle wichtig |
| 18 | Audience-Klassifikation | Pure LLM (a) | Persona-Beschreibungen liefern Kontext |
| 19 | Auth | Phase 1 lokal (a), Phase 2 Entra+Sync+Group (d) | Phasenwechsel mit Feature-Reife |
| 20 | Migration-Trigger | Feature-Reife (b) | technisch getrieben |
| 21 | Sprach-Coverage | deutsch primär, englisch sekundär (b) | GKV-Kontext + technische Begriffe |
| 22 | LLM-Architektur | konfigurierbare Provider mit Verwendungszwecken | Austauschbarkeit |
| 23 | STT-Provider | konfigurierbar ohne Junction (b) | nur ein Zweck aktuell |
| 24 | LLM-Provider-Wiederverwendung | ein Provider kann mehrere Zwecke abdecken | Pragmatik bei Einzel-Provider |
| 25 | Task-Zustände | open/in_progress/done/dropped (d) | Audit-Trail über Statuswechsel |
| 26 | Task-Assignment-Modellierung | 4 nullable FKs (b) | saubere referenzielle Integrität |
| 27 | Audio-Speicherung | keine Speicherung | DSGVO-konservativ |
| 28 | Summary-Retention | konfigurierbar pro Audience (d) | unterschiedliche Wertigkeiten |
| 29 | Recht auf Vergessen | Tombstone-Pattern (b) | pragmatisch, DSB-Klärung vor Go-Live |
| 30 | Backend-Stack | bestätigt | Java 21, Spring Boot 3, PG16, Flyway |
| 31 | Aufnahmedauer-Cap | 10 Min Soft + 15 Min Hard (c) | mit Vorwarnung |
| 32 | Author-Ausscheiden | Archiv 6 Monate + Auto-Delete (c) | Compliance-Pufferzeit |
| 33 | Konsum-UX | Kombination Suche + Liste + Recent (d) | mehrere Einstiegs-Pfade |
| 34 | Roadmap | Walking Skeleton (a) | frühes E2E-Feedback |
| 35 | Test-Daten | reale Test-Aufnahmen + Seed | pragmatisch |
| 36 | Prompt-Templates | konfigurierbar pro Autor (b) | "Personal" konsequent durchgezogen |

---

*Ende der Spezifikation. Bei der Implementierung gelten die etablierten adesso health solutions Konventionen: TDD-orientiertes Vorgehen, "Ändere NICHT die Tests"-Prinzip, Date-stamped Documentation `docs/YYYYMMDD/`, Stop-Hooks für Regressionstests.*
