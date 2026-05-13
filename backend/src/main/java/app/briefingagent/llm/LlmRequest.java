package app.briefingagent.llm;

import java.util.Objects;

public record LlmRequest(LlmPurpose purpose, String systemPrompt, String userPrompt) {

    public LlmRequest {
        Objects.requireNonNull(purpose, "purpose");
        Objects.requireNonNull(userPrompt, "userPrompt");
    }
}
