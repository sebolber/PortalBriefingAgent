package app.briefingagent.ereignis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AudioMediaTypesTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "audio/webm",
            "audio/webm;codecs=opus",
            "audio/ogg",
            "audio/mp4",
            "audio/mpeg",
            "audio/wav",
            "audio/x-wav",
            "audio/x-m4a"
    })
    void accepts_whitelisted_audio_types(String contentType) {
        assertThat(AudioMediaTypes.isAccepted(contentType)).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "application/octet-stream",
            "text/plain",
            "image/png",
            "video/mp4",
            "",
            "audio/aiff",
            "not-a-mime-type",
            "audio/webm; charset=utf-8; foo=\"unterminated"
    })
    void rejects_everything_else(String contentType) {
        assertThat(AudioMediaTypes.isAccepted(contentType)).isFalse();
    }
}
