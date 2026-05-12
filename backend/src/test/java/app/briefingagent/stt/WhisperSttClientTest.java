package app.briefingagent.stt;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.briefingagent.common.ApiException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class WhisperSttClientTest {

    private WireMockServer wireMock;
    private WhisperSttClient client;

    @BeforeEach
    void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();

        SttProviderProperties props = new SttProviderProperties();
        props.setEndpointUrl(wireMock.baseUrl() + "/v1/audio/transcriptions");
        props.setModelName("whisper-large-v3");
        props.setApiKey("");
        props.setTimeoutSeconds(5);
        client = new WhisperSttClient(props);
    }

    @AfterEach
    void stopWireMock() {
        wireMock.stop();
    }

    @Test
    void transcribe_parses_verbose_json_payload() {
        wireMock.stubFor(post(urlPathEqualTo("/v1/audio/transcriptions"))
                .withHeader("Content-Type", containing("multipart/form-data"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"text\":\"Hallo Welt\",\"language\":\"de\",\"duration\":4.7}")));

        TranscriptionResult result = client.transcribe(
                new ByteArrayInputStream("audio-bytes".getBytes()),
                "audio/webm",
                "capture.webm");

        assertThat(result.text()).isEqualTo("Hallo Welt");
        assertThat(result.language()).isEqualTo("de");
        assertThat(result.durationSeconds()).isEqualTo(5);
    }

    @Test
    void transcribe_includes_bearer_token_when_configured() {
        SttProviderProperties props = new SttProviderProperties();
        props.setEndpointUrl(wireMock.baseUrl() + "/v1/audio/transcriptions");
        props.setApiKey("secret-token");
        props.setTimeoutSeconds(5);
        WhisperSttClient secured = new WhisperSttClient(props);

        wireMock.stubFor(post(urlPathEqualTo("/v1/audio/transcriptions"))
                .withHeader("Authorization", equalTo("Bearer secret-token"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"text\":\"ok\"}")));

        TranscriptionResult r = secured.transcribe(
                new ByteArrayInputStream(new byte[]{1}), "audio/webm", "x.webm");
        assertThat(r.text()).isEqualTo("ok");
    }

    @Test
    void transcribe_handles_minimal_response_with_only_text() {
        wireMock.stubFor(post(urlPathEqualTo("/v1/audio/transcriptions"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"text\":\"only-text\"}")));

        TranscriptionResult r = client.transcribe(
                new ByteArrayInputStream(new byte[]{1}), "audio/webm", "a.webm");

        assertThat(r.text()).isEqualTo("only-text");
        assertThat(r.language()).isNull();
        assertThat(r.durationSeconds()).isNull();
    }

    @Test
    void transcribe_throws_502_on_upstream_500() {
        wireMock.stubFor(post(urlPathEqualTo("/v1/audio/transcriptions"))
                .willReturn(aResponse().withStatus(500).withBody("nope")));

        assertThatThrownBy(() -> client.transcribe(
                new ByteArrayInputStream(new byte[]{1}), "audio/webm", "a.webm"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void transcribe_throws_502_when_response_missing_text_field() {
        wireMock.stubFor(post(urlPathEqualTo("/v1/audio/transcriptions"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"language\":\"de\"}")));

        assertThatThrownBy(() -> client.transcribe(
                new ByteArrayInputStream(new byte[]{1}), "audio/webm", "a.webm"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void transcribe_uploads_payload_under_form_field_named_file() {
        wireMock.stubFor(post(urlPathEqualTo("/v1/audio/transcriptions"))
                .withRequestBody(matching("(?s).*name=\"file\".*"))
                .withRequestBody(matching("(?s).*name=\"model\".*"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"text\":\"ok\"}")));

        TranscriptionResult r = client.transcribe(
                new ByteArrayInputStream(new byte[]{1, 2, 3}),
                "audio/webm",
                "capture.webm");

        assertThat(r.text()).isEqualTo("ok");
    }
}
