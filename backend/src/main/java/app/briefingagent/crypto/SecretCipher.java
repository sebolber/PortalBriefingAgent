package app.briefingagent.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM authenticated encryption for short secrets such as API
 * keys. Each call produces a fresh 96-bit nonce; the wire format is
 * Base64-encoded {@code nonce || ciphertext-with-tag}. Decryption rejects
 * tampered or truncated input.
 *
 * <p>The master key is a 32-byte value supplied by the operator at
 * startup (see {@code SecretCipherConfig}). Losing the key permanently
 * makes the stored ciphertexts unrecoverable — the launcher persists
 * the key per host so this matches typical operational expectations.</p>
 */
public final class SecretCipher {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int NONCE_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final byte[] masterKey;
    private final SecureRandom random;

    public SecretCipher(byte[] masterKey) {
        if (masterKey == null || masterKey.length != 32) {
            throw new IllegalArgumentException("Master key must be exactly 32 bytes (AES-256).");
        }
        this.masterKey = masterKey.clone();
        this.random = new SecureRandom();
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        byte[] nonce = new byte[NONCE_LENGTH_BYTES];
        random.nextBytes(nonce);
        byte[] cipherText = cipher(Cipher.ENCRYPT_MODE, nonce, plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] envelope = new byte[NONCE_LENGTH_BYTES + cipherText.length];
        System.arraycopy(nonce, 0, envelope, 0, NONCE_LENGTH_BYTES);
        System.arraycopy(cipherText, 0, envelope, NONCE_LENGTH_BYTES, cipherText.length);
        return Base64.getEncoder().encodeToString(envelope);
    }

    public String decrypt(String envelopeBase64) {
        if (envelopeBase64 == null || envelopeBase64.isBlank()) {
            return null;
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(envelopeBase64);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Stored secret is not valid Base64", ex);
        }
        if (decoded.length <= NONCE_LENGTH_BYTES) {
            throw new IllegalArgumentException("Stored secret is too short to contain nonce + tag");
        }
        byte[] nonce = new byte[NONCE_LENGTH_BYTES];
        byte[] body = new byte[decoded.length - NONCE_LENGTH_BYTES];
        System.arraycopy(decoded, 0, nonce, 0, NONCE_LENGTH_BYTES);
        System.arraycopy(decoded, NONCE_LENGTH_BYTES, body, 0, body.length);
        byte[] plain = cipher(Cipher.DECRYPT_MODE, nonce, body);
        return new String(plain, StandardCharsets.UTF_8);
    }

    private byte[] cipher(int mode, byte[] nonce, byte[] input) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(mode, new SecretKeySpec(masterKey, ALGORITHM),
                    new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return cipher.doFinal(input);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    (mode == Cipher.ENCRYPT_MODE ? "Encryption" : "Decryption") + " failed: " + ex.getMessage(),
                    ex);
        }
    }
}
