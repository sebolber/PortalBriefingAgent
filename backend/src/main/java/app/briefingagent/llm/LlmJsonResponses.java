package app.briefingagent.llm;

import java.util.regex.Pattern;

/**
 * Normalises LLM responses before JSON parsing. Many OpenAI-compatible
 * models wrap structured output in markdown code fences (e.g.
 * {@code ```json ... ```}) even when the prompt forbids it; this helper
 * strips that envelope so the downstream parser sees plain JSON.
 */
public final class LlmJsonResponses {

    private static final Pattern FENCE_START =
            Pattern.compile("(?s)^\\s*```(?:[A-Za-z0-9_-]+)?\\s*");
    private static final Pattern FENCE_END =
            Pattern.compile("(?s)\\s*```\\s*$");

    private LlmJsonResponses() {}

    public static String stripCodeFences(String body) {
        if (body == null) {
            return null;
        }
        String trimmed = body.strip();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        String withoutStart = FENCE_START.matcher(trimmed).replaceFirst("");
        return FENCE_END.matcher(withoutStart).replaceFirst("").strip();
    }
}
