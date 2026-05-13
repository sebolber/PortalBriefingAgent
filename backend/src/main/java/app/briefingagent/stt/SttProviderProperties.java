package app.briefingagent.stt;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "briefingagent.stt")
public class SttProviderProperties {

    /**
     * Base URL of the audio-transcriptions endpoint, e.g.
     * {@code http://stt.internal.local:9000/v1/audio/transcriptions}.
     */
    @NotBlank
    private String endpointUrl = "http://localhost:9000/v1/audio/transcriptions";

    /** Whisper model identifier used by the configured backend. */
    @NotBlank
    private String modelName = "whisper-large-v3";

    /** Bearer token; empty for self-hosted Whisper that does not require auth. */
    private String apiKey = "";

    /** HTTP read timeout for the upstream call (seconds). */
    @Min(5)
    private int timeoutSeconds = 90;

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
