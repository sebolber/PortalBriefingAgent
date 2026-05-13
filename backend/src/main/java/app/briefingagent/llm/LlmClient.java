package app.briefingagent.llm;

/**
 * Abstraction over an LLM endpoint. Implementations may call an
 * OpenAI-compatible HTTP service or, for early iterations, fabricate the
 * response locally. The contract is intentionally minimal so it can grow with
 * provider configuration in later iterations.
 */
public interface LlmClient {

    /**
     * Generate a single textual response for a prompt. The {@code purpose}
     * is informational and lets implementations route, log or cache by intent.
     */
    String complete(LlmRequest request);
}
