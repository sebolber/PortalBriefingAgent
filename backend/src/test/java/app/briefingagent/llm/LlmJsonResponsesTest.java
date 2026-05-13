package app.briefingagent.llm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LlmJsonResponsesTest {

    @Test
    void strips_json_fenced_block() {
        String input = "```json\n{\"tasks\":[]}\n```";
        assertThat(LlmJsonResponses.stripCodeFences(input)).isEqualTo("{\"tasks\":[]}");
    }

    @Test
    void strips_plain_fenced_block_without_language_hint() {
        String input = "```\n{\"a\":1}\n```";
        assertThat(LlmJsonResponses.stripCodeFences(input)).isEqualTo("{\"a\":1}");
    }

    @Test
    void leaves_unfenced_json_untouched_modulo_trim() {
        String input = "  {\"a\":1}  ";
        assertThat(LlmJsonResponses.stripCodeFences(input)).isEqualTo("{\"a\":1}");
    }

    @Test
    void tolerates_leading_whitespace_before_fence() {
        String input = "\n   ```json\n{\"a\":1}\n```\n";
        assertThat(LlmJsonResponses.stripCodeFences(input)).isEqualTo("{\"a\":1}");
    }

    @Test
    void null_passes_through() {
        assertThat(LlmJsonResponses.stripCodeFences(null)).isNull();
    }

    @Test
    void inline_fence_without_newlines_is_handled() {
        String input = "```json{\"a\":1}```";
        assertThat(LlmJsonResponses.stripCodeFences(input)).isEqualTo("{\"a\":1}");
    }
}
