package app.briefingagent.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import app.briefingagent.llm.LlmPurpose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DefaultPromptContentTest {

    @ParameterizedTest
    @EnumSource(LlmPurpose.class)
    void seeded_template_contains_all_required_placeholders(LlmPurpose purpose) {
        String content = DefaultPromptContent.BY_PURPOSE.get(purpose);

        assertThat(content)
                .withFailMessage("No default content for purpose %s", purpose)
                .isNotNull();
        assertThat(PromptPlaceholders.missingFrom(purpose, content))
                .withFailMessage("Default content for %s is missing placeholders: %s",
                        purpose, PromptPlaceholders.missingFrom(purpose, content))
                .isEmpty();
    }

    @Test
    void map_covers_every_known_purpose() {
        assertThat(DefaultPromptContent.BY_PURPOSE.keySet())
                .containsExactlyInAnyOrder(LlmPurpose.values());
    }
}
