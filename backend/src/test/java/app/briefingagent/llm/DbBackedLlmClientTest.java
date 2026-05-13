package app.briefingagent.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.briefingagent.common.TestEntities;
import app.briefingagent.llm.config.LlmProvider;
import app.briefingagent.llm.config.LlmProviderUsage;
import app.briefingagent.llm.config.LlmProviderUsageRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbBackedLlmClientTest {

    @Mock LlmProviderUsageRepository usageRepository;
    @Mock HttpChatCompletionClient httpClient;
    @Mock SecretStore secretStore;
    @Mock MockLlmClient fallback;

    DbBackedLlmClient client;

    @BeforeEach
    void setUp() {
        client = new DbBackedLlmClient(usageRepository, httpClient, secretStore, fallback);
    }

    @Test
    void no_active_provider_delegates_to_mock_fallback() {
        when(usageRepository.findByPurposeAndActiveTrue(LlmPurpose.SUMMARY_GENERATION))
                .thenReturn(Optional.empty());
        when(fallback.complete(any(LlmRequest.class))).thenReturn("mock body");

        String body = client.complete(new LlmRequest(LlmPurpose.SUMMARY_GENERATION, "sys", "user"));

        assertThat(body).isEqualTo("mock body");
        verify(httpClient, never()).complete(any(), any(), any());
    }

    @Test
    void active_provider_dispatches_to_http_client_with_resolved_secret() {
        LlmProvider provider = TestEntities.withRandomId(
                new LlmProvider("Llama", "http://stub/v1/chat/completions", "llama-3.3-70b"));
        provider.setApiKeySecretRef("LLAMA_API_KEY");
        LlmProviderUsage usage = new LlmProviderUsage(provider, LlmPurpose.SUMMARY_GENERATION, true);
        when(usageRepository.findByPurposeAndActiveTrue(LlmPurpose.SUMMARY_GENERATION))
                .thenReturn(Optional.of(usage));
        when(secretStore.resolve("LLAMA_API_KEY")).thenReturn("secret-token");
        when(httpClient.complete(eq(provider), eq("secret-token"), any(LlmRequest.class)))
                .thenReturn("http body");

        LlmRequest request = new LlmRequest(LlmPurpose.SUMMARY_GENERATION, "sys", "user");
        String body = client.complete(request);

        assertThat(body).isEqualTo("http body");
        verify(fallback, never()).complete(any());
    }
}
