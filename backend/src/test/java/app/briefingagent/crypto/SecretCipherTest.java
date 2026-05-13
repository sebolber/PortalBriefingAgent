package app.briefingagent.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class SecretCipherTest {

    private static final byte[] KEY = Base64.getDecoder().decode(
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");

    private final SecretCipher cipher = new SecretCipher(KEY);

    @Test
    void encrypts_and_decrypts_round_trip() {
        String envelope = cipher.encrypt("sk-llama-secret-123");

        assertThat(envelope).isNotEqualTo("sk-llama-secret-123");
        assertThat(cipher.decrypt(envelope)).isEqualTo("sk-llama-secret-123");
    }

    @Test
    void each_encryption_uses_a_fresh_nonce() {
        String first = cipher.encrypt("same input");
        String second = cipher.encrypt("same input");

        assertThat(first).isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo("same input");
        assertThat(cipher.decrypt(second)).isEqualTo("same input");
    }

    @Test
    void tampered_ciphertext_fails_authenticated_decryption() {
        String envelope = cipher.encrypt("real secret");
        byte[] bytes = Base64.getDecoder().decode(envelope);
        bytes[bytes.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> cipher.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Decryption failed");
    }

    @Test
    void invalid_base64_fails_loudly_on_decrypt() {
        assertThatThrownBy(() -> cipher.decrypt("not base64 at all***"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void too_short_envelope_rejected() {
        String tooShort = Base64.getEncoder().encodeToString(new byte[8]);

        assertThatThrownBy(() -> cipher.decrypt(tooShort))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void null_plaintext_round_trips_as_null() {
        assertThat(cipher.encrypt(null)).isNull();
        assertThat(cipher.decrypt(null)).isNull();
        assertThat(cipher.decrypt("")).isNull();
    }

    @Test
    void rejects_wrong_size_master_key() {
        assertThatThrownBy(() -> new SecretCipher(new byte[16]))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
