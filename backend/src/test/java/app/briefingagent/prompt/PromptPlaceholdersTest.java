package app.briefingagent.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import app.briefingagent.llm.LlmPurpose;
import org.junit.jupiter.api.Test;

class PromptPlaceholdersTest {

    @Test
    void summary_purpose_requires_four_placeholders() {
        assertThat(PromptPlaceholders.requiredFor(LlmPurpose.SUMMARY_GENERATION))
                .containsExactlyInAnyOrder("transcript", "audience_name", "audience_persona", "language");
    }

    @Test
    void identifies_missing_placeholders_in_a_template() {
        String content = "Generate for {{audience_name}} in {{language}}: {{transcript}}";

        assertThat(PromptPlaceholders.missingFrom(LlmPurpose.SUMMARY_GENERATION, content))
                .containsExactly("audience_persona");
    }

    @Test
    void empty_template_lists_all_required_placeholders() {
        assertThat(PromptPlaceholders.missingFrom(LlmPurpose.AUDIENCE_CLASSIFICATION, ""))
                .containsExactlyInAnyOrder("transcript", "audiences_with_personas");
    }

    @Test
    void null_template_lists_all_required_placeholders() {
        assertThat(PromptPlaceholders.missingFrom(LlmPurpose.TASK_EXTRACTION, null))
                .containsExactlyInAnyOrder("transcript", "author_name");
    }

    @Test
    void extra_placeholders_are_ignored() {
        String content = "{{transcript}} {{author_name}} {{extra}}";

        assertThat(PromptPlaceholders.missingFrom(LlmPurpose.TASK_EXTRACTION, content)).isEmpty();
    }

    @Test
    void whitespace_inside_braces_still_matches() {
        String content = "{{ transcript }} {{author_name}}";

        assertThat(PromptPlaceholders.missingFrom(LlmPurpose.TASK_EXTRACTION, content)).isEmpty();
    }
}
