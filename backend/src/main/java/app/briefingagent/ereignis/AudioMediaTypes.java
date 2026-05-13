package app.briefingagent.ereignis;

import java.util.Set;
import org.springframework.http.MediaType;

/**
 * Whitelist of audio content types accepted by the capture endpoint.
 * Anything outside this set is rejected at the controller boundary so we
 * never hand untrusted MIME types to the Whisper service.
 */
public final class AudioMediaTypes {

    public static final MediaType AUDIO_WEBM = MediaType.valueOf("audio/webm");
    public static final MediaType AUDIO_OGG = MediaType.valueOf("audio/ogg");
    public static final MediaType AUDIO_MP4 = MediaType.valueOf("audio/mp4");
    public static final MediaType AUDIO_MPEG = MediaType.valueOf("audio/mpeg");
    public static final MediaType AUDIO_WAV = MediaType.valueOf("audio/wav");
    public static final MediaType AUDIO_X_WAV = MediaType.valueOf("audio/x-wav");
    public static final MediaType AUDIO_X_M4A = MediaType.valueOf("audio/x-m4a");

    public static final Set<MediaType> ACCEPTED = Set.of(
            AUDIO_WEBM, AUDIO_OGG, AUDIO_MP4, AUDIO_MPEG, AUDIO_WAV, AUDIO_X_WAV, AUDIO_X_M4A);

    private AudioMediaTypes() {
    }

    public static boolean isAccepted(String contentType) {
        if (contentType == null) {
            return false;
        }
        try {
            MediaType requested = MediaType.parseMediaType(contentType);
            return ACCEPTED.stream().anyMatch(allowed -> allowed.isCompatibleWith(requested));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
