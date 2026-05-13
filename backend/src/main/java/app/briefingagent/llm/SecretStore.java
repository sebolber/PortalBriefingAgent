package app.briefingagent.llm;

/**
 * Resolves a textual secret reference (as stored on a provider row) into
 * the actual secret value. Phase 1 reads environment variables; phase 2
 * may swap in a Vault-backed implementation by registering a different
 * bean (see ADR 0018).
 */
public interface SecretStore {

    /**
     * @param secretRef the value of {@code api_key_secret_ref} or any
     *                  similar column. May be {@code null} or blank, in
     *                  which case the provider expects no auth.
     * @return the resolved secret, or an empty string when no secret is
     *         configured.
     */
    String resolve(String secretRef);
}
