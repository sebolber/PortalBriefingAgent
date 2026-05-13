package app.briefingagent.stt;

import java.util.Objects;

/**
 * Outcome of a successful transcription call.
 *
 * @param text     the transcribed text, never {@code null}
 * @param language ISO-639-1 language code reported by the provider, or
 *                 {@code null} if unknown
 * @param durationSeconds duration of the original audio in seconds, or
 *                 {@code null} if the provider did not report it
 */
public record TranscriptionResult(String text, String language, Integer durationSeconds) {

    public TranscriptionResult {
        Objects.requireNonNull(text, "text");
    }
}
