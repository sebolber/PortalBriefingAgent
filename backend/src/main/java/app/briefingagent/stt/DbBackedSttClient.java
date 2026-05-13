package app.briefingagent.stt;

import app.briefingagent.common.ApiException;
import app.briefingagent.crypto.SecretCipher;
import app.briefingagent.llm.SecretStore;
import app.briefingagent.stt.config.SttProvider;
import app.briefingagent.stt.config.SttProviderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Routes audio transcription to the active STT provider configured in
 * {@code stt_provider}. Falls back to {@link WhisperSttClient} (the
 * property-based default) when no DB provider is active.
 */
@Component
@Primary
public class DbBackedSttClient implements SttProviderClient {

    private static final Logger LOG = LoggerFactory.getLogger(DbBackedSttClient.class);
    private static final String FIELD_FILE = "file";
    private static final String FIELD_MODEL = "model";
    private static final String FIELD_RESPONSE_FORMAT = "response_format";
    private static final String FORMAT_VERBOSE_JSON = "verbose_json";
    private static final String FIELD_DURATION = "duration";

    private final SttProviderRepository repository;
    private final SecretStore secretStore;
    private final SecretCipher secretCipher;
    private final WhisperSttClient fallback;

    public DbBackedSttClient(SttProviderRepository repository,
                             SecretStore secretStore,
                             SecretCipher secretCipher,
                             WhisperSttClient fallback) {
        this.repository = repository;
        this.secretStore = secretStore;
        this.secretCipher = secretCipher;
        this.fallback = fallback;
    }

    @Override
    @Transactional(readOnly = true)
    public TranscriptionResult transcribe(InputStream audio, String contentType, String filename) {
        Optional<SttProvider> active = repository.findFirstByActiveTrue();
        if (active.isEmpty()) {
            LOG.debug("No active STT provider — falling back to property-configured client.");
            return fallback.transcribe(audio, contentType, filename);
        }
        SttProvider provider = active.get();
        return callProvider(provider, resolveApiKey(provider), audio, contentType, filename);
    }

    private String resolveApiKey(SttProvider provider) {
        String encrypted = provider.getApiKeyEncrypted();
        if (encrypted != null && !encrypted.isBlank()) {
            return secretCipher.decrypt(encrypted);
        }
        return secretStore.resolve(provider.getApiKeySecretRef());
    }

    private TranscriptionResult callProvider(SttProvider provider, String apiKey,
                                             InputStream audio, String contentType,
                                             String filename) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(180));
        RestClient client = RestClient.builder()
                .baseUrl(provider.getEndpointUrl())
                .requestFactory(factory)
                .build();

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add(FIELD_MODEL, provider.getModelName());
        form.add(FIELD_RESPONSE_FORMAT, FORMAT_VERBOSE_JSON);
        form.add(FIELD_FILE, audioPart(audio, contentType, filename));

        JsonNode response;
        try {
            response = client.post()
                    .headers(headers -> {
                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                        if (apiKey != null && !apiKey.isBlank()) {
                            headers.setBearerAuth(apiKey);
                        }
                    })
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            LOG.warn("STT call to {} failed: {}", provider.getEndpointUrl(), ex.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Speech-to-Text provider unavailable");
        }
        if (response == null || !response.hasNonNull("text")) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Speech-to-Text response missing 'text'");
        }
        String text = response.get("text").asText();
        String language = response.has("language") ? response.get("language").asText(null) : null;
        Integer duration = response.has(FIELD_DURATION) && response.get(FIELD_DURATION).isNumber()
                ? (int) Math.round(response.get(FIELD_DURATION).asDouble())
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
                return -1;
            }

            @Override
            public String toString() {
                return "audio[" + contentType + "]";
            }
        };
    }
}
