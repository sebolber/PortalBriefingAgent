package app.briefingagent.prompt;

import app.briefingagent.llm.LlmPurpose;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The mandatory placeholder set per purpose. Templates that omit a
 * required placeholder are rejected at the service boundary.
 */
public final class PromptPlaceholders {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z_]+)\\s*}}");

    private static final Map<LlmPurpose, Set<String>> REQUIRED = Map.of(
            LlmPurpose.AUDIENCE_CLASSIFICATION, Set.of("transcript", "audiences_with_personas"),
            LlmPurpose.SUMMARY_GENERATION, Set.of("transcript", "audience_name", "audience_persona", "language"),
            LlmPurpose.TASK_EXTRACTION, Set.of("transcript", "author_name"),
            LlmPurpose.TRANSCRIPT_CORRECTION, Set.of("transcript"));

    private PromptPlaceholders() {
    }

    public static Set<String> requiredFor(LlmPurpose purpose) {
        return REQUIRED.getOrDefault(purpose, Set.of());
    }

    public static List<String> missingFrom(LlmPurpose purpose, String content) {
        Set<String> required = new HashSet<>(requiredFor(purpose));
        if (content == null) {
            return List.copyOf(required);
        }
        Matcher m = PLACEHOLDER_PATTERN.matcher(content);
        while (m.find()) {
            required.remove(m.group(1));
        }
        return required.stream().sorted().toList();
    }
}
