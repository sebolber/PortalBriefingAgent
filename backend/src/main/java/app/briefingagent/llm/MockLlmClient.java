package app.briefingagent.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Local-only mock used by Iter 0 to keep the walking skeleton independent
 * from a running LLM service. Returns a deterministic Markdown summary
 * derived from the request body so dashboard rendering and edge-case tests
 * stay reproducible.
 */
@Component
@ConditionalOnProperty(prefix = "briefingagent.llm.mock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MockLlmClient implements LlmClient {

    private static final int DEFAULT_PREVIEW_CHARS = 280;
    private static final String NEWLINE = "\n";

    @Override
    public String complete(LlmRequest request) {
        String trimmed = request.userPrompt().strip();
        String preview = trimmed.length() <= DEFAULT_PREVIEW_CHARS
                ? trimmed
                : trimmed.substring(0, DEFAULT_PREVIEW_CHARS) + "…";
        return switch (request.purpose()) {
            case SUMMARY_GENERATION -> renderSummary(preview);
            case AUDIENCE_CLASSIFICATION -> "{\"audiences\":[],\"confidence\":\"low\",\"reasoning\":\"mock\"}";
            case TASK_EXTRACTION -> "{\"tasks\":[]}";
            case TRANSCRIPT_CORRECTION -> trimmed;
        };
    }

    private static String renderSummary(String preview) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Mock-Summary").append(NEWLINE).append(NEWLINE);
        sb.append("Diese Summary stammt aus dem lokalen Mock-LLM. ");
        sb.append("Auszug aus dem Original:").append(NEWLINE).append(NEWLINE);
        sb.append("> ").append(preview.replace(NEWLINE, NEWLINE + "> "));
        return sb.toString();
    }
}
