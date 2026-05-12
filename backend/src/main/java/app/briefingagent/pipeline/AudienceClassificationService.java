package app.briefingagent.pipeline;

import app.briefingagent.audience.AudienceRef;
import app.briefingagent.llm.LlmClient;
import app.briefingagent.llm.LlmPurpose;
import app.briefingagent.llm.LlmRequest;
import app.briefingagent.summary.ClassificationConfidence;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Asks the configured LLM which audiences the transcript is relevant for,
 * then maps the response back to the author's audience refs. Unknown ids,
 * malformed JSON or empty lists never throw upward — the caller deals with
 * an empty result by attaching a fallback summary.
 */
@Service
public class AudienceClassificationService {

    private static final Logger LOG = LoggerFactory.getLogger(AudienceClassificationService.class);

    private static final String SYSTEM_PROMPT = """
            Du bist ein Klassifikations-Agent für ein internes Briefing-Tool.
            Wähle aus der Liste der vorgegebenen Audiences nur diejenigen aus,
            die in dem Transkript ausdrücklich oder klar zwischen den Zeilen
            adressiert werden. Antwort ausschließlich als JSON, ohne Markdown,
            ohne Erläuterung.
            """;

    private final LlmClient llmClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public AudienceClassificationService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public List<AudienceMatch> classify(String transcript, List<AudienceRef> audiences) {
        if (audiences.isEmpty() || transcript == null || transcript.isBlank()) {
            return List.of();
        }
        String userPrompt = buildPrompt(transcript, audiences);
        String response = llmClient.complete(new LlmRequest(
                LlmPurpose.AUDIENCE_CLASSIFICATION, SYSTEM_PROMPT, userPrompt));
        return parse(response, audiences);
    }

    private String buildPrompt(String transcript, List<AudienceRef> audiences) {
        StringBuilder sb = new StringBuilder();
        sb.append("Transkript:\n").append(transcript).append("\n\n");
        sb.append("Audiences (id|typ|name|persona):\n");
        for (AudienceRef ref : audiences) {
            sb.append("- ").append(ref.id()).append('|').append(ref.type().dbValue())
                    .append('|').append(ref.name()).append('|')
                    .append(personaPreview(ref.personaText())).append('\n');
        }
        sb.append("\nAntwortformat:\n");
        sb.append("{\"audiences\":[{\"id\":\"<uuid>\",\"type\":\"person|persongroup|topic\",")
                .append("\"confidence\":\"low|medium|high\",\"reasoning\":\"<1 Satz>\"}]}");
        return sb.toString();
    }

    private static String personaPreview(String text) {
        if (text == null) {
            return "";
        }
        String stripped = text.replace('\n', ' ').replace('\r', ' ').strip();
        return stripped.length() > 240 ? stripped.substring(0, 240) + "…" : stripped;
    }

    private List<AudienceMatch> parse(String response, List<AudienceRef> audiences) {
        if (response == null || response.isBlank()) {
            LOG.warn("Classifier returned empty body");
            return List.of();
        }
        JsonNode root;
        try {
            root = mapper.readTree(response);
        } catch (Exception ex) {
            LOG.warn("Classifier returned malformed JSON: {}", ex.getMessage());
            return List.of();
        }
        JsonNode array = root.get("audiences");
        if (array == null || !array.isArray()) {
            return List.of();
        }
        Map<UUID, AudienceRef> byId = new HashMap<>();
        audiences.forEach(a -> byId.put(a.id(), a));

        List<AudienceMatch> matches = new ArrayList<>();
        for (JsonNode entry : array) {
            String idStr = textOrNull(entry, "id");
            String typeStr = textOrNull(entry, "type");
            String confStr = textOrNull(entry, "confidence");
            String reasoning = textOrNull(entry, "reasoning");
            if (idStr == null) {
                continue;
            }
            UUID id;
            try {
                id = UUID.fromString(idStr);
            } catch (IllegalArgumentException ex) {
                LOG.debug("Classifier returned non-UUID id '{}'", idStr);
                continue;
            }
            AudienceRef ref = byId.get(id);
            if (ref == null) {
                LOG.debug("Classifier referenced unknown audience id {}", id);
                continue;
            }
            if (typeStr != null && !typeStr.equalsIgnoreCase(ref.type().dbValue())) {
                LOG.debug("Classifier returned mismatched type '{}' for audience {}", typeStr, id);
                continue;
            }
            ClassificationConfidence confidence = parseConfidence(confStr);
            matches.add(new AudienceMatch(ref, confidence, reasoning));
        }
        return matches;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private static ClassificationConfidence parseConfidence(String value) {
        if (value == null) {
            return ClassificationConfidence.LOW;
        }
        return switch (value.toLowerCase()) {
            case "high" -> ClassificationConfidence.HIGH;
            case "medium" -> ClassificationConfidence.MEDIUM;
            default -> ClassificationConfidence.LOW;
        };
    }

}
