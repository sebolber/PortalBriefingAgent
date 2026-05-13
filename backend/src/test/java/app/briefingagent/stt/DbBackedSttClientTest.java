package app.briefingagent.stt;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.briefingagent.common.TestEntities;
import app.briefingagent.llm.SecretStore;
import app.briefingagent.stt.config.SttProvider;
import app.briefingagent.stt.config.SttProviderRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbBackedSttClientTest {

    @Mock SttProviderRepository repository;
    @Mock SecretStore secretStore;
    @Mock app.briefingagent.crypto.SecretCipher secretCipher;
    @Mock WhisperSttClient fallback;

    DbBackedSttClient client;
    WireMockServer wireMock;

    @BeforeEach
    void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        client = new DbBackedSttClient(repository, secretStore, secretCipher, fallback);
    }

    @AfterEach
    void stopWireMock() {
        wireMock.stop();
    }

    @Test
    void no_active_provider_delegates_to_property_based_fallback() {
        when(repository.findFirstByActiveTrue()).thenReturn(Optional.empty());
        when(fallback.transcribe(any(), any(), any()))
                .thenReturn(new TranscriptionResult("falling back", "de", 1));

        TranscriptionResult result = client.transcribe(
                new ByteArrayInputStream(new byte[]{1}), "audio/webm", "x.webm");

        assertThat(result.text()).isEqualTo("falling back");
    }

    @Test
    void active_provider_dispatches_via_http_with_resolved_secret() {
        wireMock.stubFor(post(urlPathEqualTo("/v1/audio/transcriptions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"text\":\"DB Whisper\",\"language\":\"de\",\"duration\":3.4}")));
        SttProvider provider = TestEntities.withRandomId(
                new SttProvider("Whisper-DB", wireMock.baseUrl() + "/v1/audio/transcriptions", "whisper-large-v3"));
        provider.setApiKeySecretRef("WHISPER_KEY");
        provider.setActive(true);
        when(repository.findFirstByActiveTrue()).thenReturn(Optional.of(provider));
        when(secretStore.resolve("WHISPER_KEY")).thenReturn("super-secret");

        TranscriptionResult result = client.transcribe(
                new ByteArrayInputStream(new byte[]{1, 2, 3}), "audio/webm", "x.webm");

        assertThat(result.text()).isEqualTo("DB Whisper");
        assertThat(result.language()).isEqualTo("de");
        assertThat(result.durationSeconds()).isEqualTo(3);
        verify(fallback, never()).transcribe(any(), any(), any());
    }
}
