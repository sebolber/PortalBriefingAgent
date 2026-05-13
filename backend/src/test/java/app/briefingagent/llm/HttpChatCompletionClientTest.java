package app.briefingagent.llm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HttpChatCompletionClientTest {

    @Test
    void base_url_without_chat_completions_gets_the_path_appended() {
        assertThat(HttpChatCompletionClient.chatCompletionsUrl("https://host/v1"))
                .isEqualTo("https://host/v1/chat/completions");
    }

    @Test
    void trailing_slash_on_base_url_is_normalised() {
        assertThat(HttpChatCompletionClient.chatCompletionsUrl("https://host/v1/"))
                .isEqualTo("https://host/v1/chat/completions");
    }

    @Test
    void explicit_chat_completions_url_is_preserved() {
        assertThat(HttpChatCompletionClient.chatCompletionsUrl("https://host/v1/chat/completions"))
                .isEqualTo("https://host/v1/chat/completions");
    }

    @Test
    void explicit_chat_completions_url_with_trailing_slash_is_normalised() {
        assertThat(HttpChatCompletionClient.chatCompletionsUrl("https://host/v1/chat/completions/"))
                .isEqualTo("https://host/v1/chat/completions");
    }

    @Test
    void blank_or_null_passes_through_for_the_caller_to_handle() {
        assertThat(HttpChatCompletionClient.chatCompletionsUrl(null)).isNull();
        assertThat(HttpChatCompletionClient.chatCompletionsUrl("")).isEmpty();
    }

    @Test
    void custom_path_still_gets_chat_completions_appended() {
        // Some on-prem gateways expose the chat completion under a custom
        // prefix; treat anything that does not already end with the
        // canonical path as a base URL.
        assertThat(HttpChatCompletionClient.chatCompletionsUrl("https://host/api/v2"))
                .isEqualTo("https://host/api/v2/chat/completions");
    }
}
