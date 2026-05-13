package app.briefingagent.crypto;

import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the application's {@link SecretCipher}. The master key is a
 * 32-byte AES-256 key supplied as Base64 via the
 * {@code briefingagent.crypto.secret-key} property — which Spring
 * automatically maps from the {@code BRIEFINGAGENT_SECRET_KEY}
 * environment variable. The launcher (scripts/run.sh) generates one
 * per host and persists it under {@code ~/.briefingagent/secret-key}
 * so operators don't have to manage it manually for dev installs.
 *
 * <p>If the property is missing the bean throws an
 * {@link IllegalStateException} during context refresh, which surfaces
 * as a clear startup error rather than letting the application boot
 * into an inconsistent state.</p>
 */
@Configuration(proxyBeanMethods = false)
public class SecretCipherConfig {

    private static final String KEY_PROPERTY = "briefingagent.crypto.secret-key";

    @Bean
    public SecretCipher secretCipher(
            @Value("${" + KEY_PROPERTY + ":}") String secretKeyBase64) {
        if (secretKeyBase64 == null || secretKeyBase64.isBlank()) {
            throw new IllegalStateException(
                    "Missing encryption master key. Set BRIEFINGAGENT_SECRET_KEY to a 32-byte "
                            + "AES-256 key (Base64 encoded). The launcher script (scripts/run.sh) "
                            + "generates one for you on first run.");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secretKeyBase64.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "BRIEFINGAGENT_SECRET_KEY is not valid Base64.", ex);
        }
        return new SecretCipher(decoded);
    }
}
