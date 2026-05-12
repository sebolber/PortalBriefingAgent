package app.briefingagent.summary;

import app.briefingagent.common.ApiException;
import app.briefingagent.llm.LlmClient;
import app.briefingagent.llm.LlmPurpose;
import app.briefingagent.llm.LlmRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Encapsulates the review actions a user performs on a generated summary:
 * manual edits, LLM regeneration with feedback, and acceptance. All
 * non-trivial transitions append an {@link EditHistoryEntry} so the audit
 * trail stays append-only.
 */
@Service
public class SummaryReviewService {

    private static final String REGENERATE_SYSTEM_PROMPT_TEMPLATE = """
            Du bist ein Briefing-Assistent. Verbessere die vorhandene Summary
            anhand des Autor-Feedbacks. Audience: %s. Persona: %s.

            Halte Stil, Sprache und Markdown bei. Antwort: nur die neue Summary.
            """;

    private final SummaryRepository repository;
    private final LlmClient llmClient;

    public SummaryReviewService(SummaryRepository repository, LlmClient llmClient) {
        this.repository = repository;
        this.llmClient = llmClient;
    }

    @Transactional
    public Summary edit(UUID summaryId, UUID authorId, String newText) {
        Summary summary = loadEditable(summaryId);
        String trimmed = newText == null ? "" : newText.strip();
        if (trimmed.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Summary text must not be empty");
        }
        if (trimmed.equals(summary.getSummaryText())) {
            return summary;
        }
        summary.appendHistory(new EditHistoryEntry(
                EditHistoryEntry.TYPE_MANUAL_EDIT,
                Instant.now(),
                authorId,
                summary.getSummaryText(),
                trimmed,
                null));
        summary.setSummaryText(trimmed);
        summary.setEditState(EditState.MANUALLY_EDITED);
        return repository.save(summary);
    }

    @Transactional
    public Summary regenerate(UUID summaryId, UUID authorId, String feedback) {
        Summary summary = loadEditable(summaryId);
        String trimmedFeedback = feedback == null ? "" : feedback.strip();
        String audienceName = audienceName(summary);
        String personaText = audiencePersona(summary);

        String systemPrompt = String.format(REGENERATE_SYSTEM_PROMPT_TEMPLATE,
                audienceName,
                personaText == null ? "(keine Persona hinterlegt)" : personaText);
        String userPrompt = trimmedFeedback.isEmpty()
                ? summary.getEreignis().getTranscriptText()
                : "Feedback: " + trimmedFeedback + "\n\nTranskript:\n" + summary.getEreignis().getTranscriptText();

        String newBody = llmClient.complete(new LlmRequest(
                LlmPurpose.SUMMARY_GENERATION, systemPrompt, userPrompt));

        summary.appendHistory(new EditHistoryEntry(
                EditHistoryEntry.TYPE_REGENERATED,
                Instant.now(),
                authorId,
                summary.getSummaryText(),
                newBody,
                trimmedFeedback.isEmpty() ? null : trimmedFeedback));
        summary.setSummaryText(newBody);
        summary.setEditState(EditState.REGENERATED);
        summary.setRegenerationFeedback(trimmedFeedback.isEmpty() ? null : trimmedFeedback);
        return repository.save(summary);
    }

    @Transactional
    public Summary accept(UUID summaryId) {
        Summary summary = load(summaryId);
        if (summary.getAcceptedAt() != null) {
            return summary;
        }
        summary.setAcceptedAt(Instant.now());
        return repository.save(summary);
    }

    private Summary loadEditable(UUID id) {
        Summary summary = load(id);
        if (summary.getAcceptedAt() != null) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Summary is already accepted; edits are no longer allowed");
        }
        return summary;
    }

    private Summary load(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Summary not found"));
    }

    private static String audienceName(Summary s) {
        return switch (s.getAudienceType()) {
            case TOPIC -> s.getAudienceTopic().getName();
            case PERSON -> s.getAudiencePerson().getDisplayName();
            case PERSONGROUP -> s.getAudiencePersonGroup().getName();
        };
    }

    private static String audiencePersona(Summary s) {
        return switch (s.getAudienceType()) {
            case TOPIC -> s.getAudienceTopic().getPersonaText();
            case PERSONGROUP -> s.getAudiencePersonGroup().getPersonaText();
            case PERSON -> null;
        };
    }
}
