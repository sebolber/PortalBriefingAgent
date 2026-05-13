package app.briefingagent.stt;

import app.briefingagent.common.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * OpenAI-compatible Whisper client implemented on top of Spring
 * {@link RestClient}. Audio is forwarded directly from the upload's input
 * stream without being copied to a persistent location; the controller
 * is responsible for closing the stream after the call returns.
 */
@Component
@ConditionalOnProperty(prefix = "briefingagent.stt", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WhisperSttClient implements SttProviderClient {

    private static final Logger LOG = LoggerFactory.getLogger(WhisperSttClient.class);
    private static final String FIELD_FILE = "file";
    private static final String FIELD_MODEL = "model";
    private static final String FIELD_RESPONSE_FORMAT = "response_format";
    private static final String FORMAT_VERBOSE_JSON = "verbose_json";
    private static final String FIELD_DURATION = "duration";

    private final SttProviderProperties properties;
    private final RestClient restClient;

    public WhisperSttClient(SttProviderProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(properties.getTimeoutSeconds());
        factory.setReadTimeout(timeout);
        factory.setConnectTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl(properties.getEndpointUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public TranscriptionResult transcribe(InputStream audio, String contentType, String filename) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add(FIELD_MODEL, properties.getModelName());
        form.add(FIELD_RESPONSE_FORMAT, FORMAT_VERBOSE_JSON);
        form.add(FIELD_FILE, audioPart(audio, contentType, filename));

        JsonNode body;
        try {
            body = restClient.post()
                    .headers(headers -> {
                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                        if (!properties.getApiKey().isBlank()) {
                            headers.setBearerAuth(properties.getApiKey());
                        }
                    })
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            LOG.warn("Whisper call failed: {}", ex.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Speech-to-Text provider unavailable");
        }
        if (body == null || !body.hasNonNull("text")) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Speech-to-Text response missing 'text'");
        }
        String text = body.get("text").asText();
        String language = body.has("language") ? body.get("language").asText(null) : null;
        Integer duration = body.has(FIELD_DURATION) && body.get(FIELD_DURATION).isNumber()
                ? (int) Math.round(body.get(FIELD_DURATION).asDouble())
                : null;
        return new TranscriptionResult(text, language, duration);
    }

    private static InputStreamResource audioPart(InputStream stream, String contentType, String filename) {
        return new InputStreamResource(stream) {

            @Override
            public String getFilename() {
                return filename;
            }

            @Override
            public long contentLength() throws IOException {
                // Streaming upload: forward the length only if the caller already knows it.
                // Returning -1 makes RestClient/Tomcat use chunked transfer encoding.
                return -1;
            }

            @Override
            public String toString() {
                return "audio[" + contentType + "]";
            }
        };
    }
}
