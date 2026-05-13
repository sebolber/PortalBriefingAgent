package app.briefingagent.llm;

import app.briefingagent.common.ApiException;
import app.briefingagent.llm.config.LlmProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Performs an OpenAI-compatible chat-completions roundtrip against the
 * provider's configured endpoint. Read timeouts are wide because LLM
 * latency is the dominant cost; connect timeouts are short to fail fast
 * on a misconfigured URL.
 */
@Component
public class HttpChatCompletionClient {

    private static final Logger LOG = LoggerFactory.getLogger(HttpChatCompletionClient.class);
    private static final String FIELD_MODEL = "model";
    private static final String FIELD_MESSAGES = "messages";
    private static final String FIELD_CHOICES = "choices";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_ROLE = "role";
    private static final String ROLE_SYSTEM = "system";
    private static final String ROLE_USER = "user";

    private final ObjectMapper mapper = new ObjectMapper();

    public String complete(LlmProvider provider, String apiKey, LlmRequest request) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(180));
        RestClient client = RestClient.builder()
                .baseUrl(provider.getEndpointUrl())
                .requestFactory(factory)
                .build();

        ObjectNode body = mapper.createObjectNode();
        body.put(FIELD_MODEL, provider.getModelName());
        ArrayNode messages = body.putArray(FIELD_MESSAGES);
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            ObjectNode sys = messages.addObject();
            sys.put(FIELD_ROLE, ROLE_SYSTEM);
            sys.put(FIELD_CONTENT, request.systemPrompt());
        }
        ObjectNode user = messages.addObject();
        user.put(FIELD_ROLE, ROLE_USER);
        user.put(FIELD_CONTENT, request.userPrompt());
        applyParameters(body, provider.getParameters());

        try {
            JsonNode response = client.post()
                    .headers(headers -> {
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        if (apiKey != null && !apiKey.isBlank()) {
                            headers.setBearerAuth(apiKey);
                        }
                    })
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return extractText(response);
        } catch (RestClientException ex) {
            LOG.warn("LLM call to {} failed: {}", provider.getEndpointUrl(), ex.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM provider unreachable");
        }
    }

    private String extractText(JsonNode response) {
        if (response == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM response empty");
        }
        JsonNode choices = response.get(FIELD_CHOICES);
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM response missing choices");
        }
        JsonNode message = choices.get(0).get(FIELD_MESSAGE);
        if (message == null || !message.hasNonNull(FIELD_CONTENT)) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM response missing content");
        }
        return message.get(FIELD_CONTENT).asText();
    }

    private void applyParameters(ObjectNode body, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            body.set(entry.getKey(), mapper.valueToTree(entry.getValue()));
        }
    }
}
