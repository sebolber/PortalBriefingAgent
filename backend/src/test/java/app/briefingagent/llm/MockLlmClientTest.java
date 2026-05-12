package app.briefingagent.llm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MockLlmClientTest {

    private final MockLlmClient client = new MockLlmClient();

    @Test
    void summary_call_returns_markdown_block() {
        String out = client.complete(new LlmRequest(LlmPurpose.SUMMARY_GENERATION, null,
                "Heute ein Workshop mit Anna besprochen."));

        assertThat(out)
                .startsWith("## Mock-Summary")
                .contains("Anna besprochen");
    }

    @Test
    void classification_call_returns_valid_empty_json() {
        String out = client.complete(new LlmRequest(LlmPurpose.AUDIENCE_CLASSIFICATION, null, "irgendwas"));

        assertThat(out).isEqualTo("{\"audiences\":[],\"confidence\":\"low\",\"reasoning\":\"mock\"}");
    }

    @Test
    void task_extraction_call_returns_empty_array() {
        String out = client.complete(new LlmRequest(LlmPurpose.TASK_EXTRACTION, null, "irgendwas"));

        assertThat(out).isEqualTo("{\"tasks\":[]}");
    }

    @Test
    void preview_truncates_long_input_with_ellipsis() {
        String huge = "x".repeat(2_000);

        String out = client.complete(new LlmRequest(LlmPurpose.SUMMARY_GENERATION, null, huge));

        assertThat(out).contains("…");
    }

    @Test
    void leading_whitespace_is_stripped_in_preview() {
        String input = "   Anna   ";

        String out = client.complete(new LlmRequest(LlmPurpose.SUMMARY_GENERATION, null, input));

        assertThat(out).contains("Anna");
        assertThat(out).doesNotContain("   Anna");
    }
}
