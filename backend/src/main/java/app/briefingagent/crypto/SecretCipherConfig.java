package app.briefingagent.crypto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the application's {@link SecretCipher}. The 32-byte AES-256
 * master key is resolved in this order:
 *
 * <ol>
 *   <li>property {@code briefingagent.crypto.secret-key} (typically fed
 *       from the {@code BRIEFINGAGENT_SECRET_KEY} environment variable
 *       — the path the launcher script uses);</li>
 *   <li>file {@code ~/.briefingagent/secret-key} — the per-host file
 *       the launcher writes on first run. Reading it directly means
 *       running the jar without the launcher works once the file
 *       exists.</li>
 * </ol>
 *
 * <p>If neither source supplies a key the bean throws an
 * {@link IllegalStateException} during context refresh so the
 * application fails fast rather than booting with a half-configured
 * crypto stack.</p>
 */
@Configuration(proxyBeanMethods = false)
public class SecretCipherConfig {

    private static final Logger log = LoggerFactory.getLogger(SecretCipherConfig.class);
    private static final String KEY_PROPERTY = "briefingagent.crypto.secret-key";
    private static final Path FALLBACK_KEY_FILE =
            Paths.get(System.getProperty("user.home"), ".briefingagent", "secret-key");

    @Bean
    public SecretCipher secretCipher(
            @Value("${" + KEY_PROPERTY + ":}") String secretKeyBase64) {
        String key = (secretKeyBase64 == null || secretKeyBase64.isBlank())
                ? readKeyFile()
                : secretKeyBase64;
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "Missing encryption master key. Either set BRIEFINGAGENT_SECRET_KEY to a "
                            + "Base64-encoded 32-byte AES-256 key, or create the file "
                            + FALLBACK_KEY_FILE + " (chmod 600) containing that key. The "
                            + "launcher script (scripts/run.sh) generates one for you on first run.");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(key.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Encryption master key is not valid Base64.", ex);
        }
        return new SecretCipher(decoded);
    }

    private static String readKeyFile() {
        if (!Files.isRegularFile(FALLBACK_KEY_FILE)) {
            return null;
        }
        try {
            String content = Files.readString(FALLBACK_KEY_FILE).trim();
            log.info("Loaded encryption master key from {}", FALLBACK_KEY_FILE);
            return content;
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Found " + FALLBACK_KEY_FILE + " but could not read it.", ex);
        }
    }
}
