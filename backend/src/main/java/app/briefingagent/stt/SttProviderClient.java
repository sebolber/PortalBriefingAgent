package app.briefingagent.stt;

import java.io.InputStream;

/**
 * Abstraction over a Speech-to-Text provider. Iter 1 uses an
 * OpenAI-compatible audio-transcriptions endpoint (Whisper). The
 * configured provider is selected at boot time; full runtime provider
 * configuration arrives with the LLM/STT provider tables in Iter 5.
 */
public interface SttProviderClient {

    /**
     * Transcribe an audio stream. The caller is responsible for closing
     * the input stream; implementations must not persist it to disk.
     *
     * @param audio          the raw audio payload (will be streamed once)
     * @param contentType    the audio MIME type (e.g. {@code audio/webm})
     * @param filename       an opaque filename hint for the provider
     * @return the transcription result, never {@code null}
     */
    TranscriptionResult transcribe(InputStream audio, String contentType, String filename);
}
