package app.briefingagent.prompt;

import app.briefingagent.llm.LlmPurpose;
import java.util.Map;

/**
 * Initial prompt content seeded for every author on creation. The texts
 * intentionally mention the spec-mandated placeholder set per purpose
 * (see {@link PromptPlaceholders}) so the validator accepts them and the
 * runtime substitution finds every variable it needs.
 */
public final class DefaultPromptContent {

    public static final String AUDIENCE_CLASSIFICATION = """
            Du bist ein Klassifikations-Agent für ein internes Briefing-Tool.
            Wähle aus der Liste der vorgegebenen Audiences ausschließlich diejenigen
            aus, die im Transkript ausdrücklich oder klar zwischen den Zeilen
            adressiert werden. Antwort nur als JSON, kein Markdown, keine
            Erläuterung.

            Audiences:
            {{audiences_with_personas}}

            Transkript:
            {{transcript}}

            Antwortformat:
            {"audiences":[{"id":"<uuid>","type":"person|persongroup|topic","confidence":"low|medium|high","reasoning":"<1 Satz>"}]}
            """;

    public static final String SUMMARY_GENERATION = """
            Du bist ein Briefing-Assistent. Erstelle eine prägnante deutsche
            Zusammenfassung im Markdown-Format speziell für folgende Audience:

            Name: {{audience_name}}
            Persona: {{audience_persona}}
            Sprache: {{language}}

            Halte dich an Stil und Fokus der Persona-Beschreibung. Antwort:
            Markdown, keine Erklärungen, keine Code-Blöcke.

            Transkript:
            {{transcript}}
            """;

    public static final String TASK_EXTRACTION = """
            Du bist ein Aufgaben-Extraktor für ein internes Briefing-Tool.
            Lies das Transkript und liefere alle Aufgaben, die aus dem Termin
            entstanden sind, in JSON. Wenn keine Aufgabe entstanden ist,
            liefere eine leere Liste.

            Autor: {{author_name}}
            Transkript:
            {{transcript}}

            Antwortformat:
            {"tasks":[{"title":"<kurz>","description":"<optional>","due_date":"YYYY-MM-DD oder null","assignee":"self|person:<uuid>|persongroup:<uuid>|topic:<uuid>"}]}
            """;

    public static final String TRANSCRIPT_CORRECTION = """
            Du bist ein Korrektur-Assistent. Übernimm das Whisper-Transkript
            und korrigiere Fachbegriffe, Eigennamen und offensichtliche
            Hörfehler. Behalte den Inhalt 1:1 bei, kürze nichts.

            Transkript:
            {{transcript}}
            """;

    public static final Map<LlmPurpose, String> BY_PURPOSE = Map.of(
            LlmPurpose.AUDIENCE_CLASSIFICATION, AUDIENCE_CLASSIFICATION,
            LlmPurpose.SUMMARY_GENERATION, SUMMARY_GENERATION,
            LlmPurpose.TASK_EXTRACTION, TASK_EXTRACTION,
            LlmPurpose.TRANSCRIPT_CORRECTION, TRANSCRIPT_CORRECTION);

    private DefaultPromptContent() {
    }
}
