package app.briefingagent.llm;

import app.briefingagent.crypto.SecretCipher;
import app.briefingagent.llm.config.LlmProvider;
import app.briefingagent.llm.config.LlmProviderUsage;
import app.briefingagent.llm.config.LlmProviderUsageRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The runtime LLM router. For each request:
 *
 * <ol>
 *   <li>Look up the {@code llm_provider_usage} row marked active for the
 *       request's {@link LlmPurpose}.</li>
 *   <li>If found, dispatch to the configured provider's HTTP endpoint via
 *       {@link HttpChatCompletionClient} with a SecretStore-resolved bearer
 *       key.</li>
 *   <li>Otherwise, fall back to the local {@link MockLlmClient} so the
 *       walking-skeleton flow stays usable when the provider tables are
 *       still empty.</li>
 * </ol>
 *
 * Annotated {@code @Primary} so the rest of the application keeps
 * injecting {@link LlmClient} without knowing about the routing layer.
 */
@Component
@Primary
public class DbBackedLlmClient implements LlmClient {

    private static final Logger LOG = LoggerFactory.getLogger(DbBackedLlmClient.class);

    private final LlmProviderUsageRepository usageRepository;
    private final HttpChatCompletionClient httpClient;
    private final SecretStore secretStore;
    private final SecretCipher secretCipher;
    private final MockLlmClient fallback;

    public DbBackedLlmClient(LlmProviderUsageRepository usageRepository,
                             HttpChatCompletionClient httpClient,
                             SecretStore secretStore,
                             SecretCipher secretCipher,
                             MockLlmClient fallback) {
        this.usageRepository = usageRepository;
        this.httpClient = httpClient;
        this.secretStore = secretStore;
        this.secretCipher = secretCipher;
        this.fallback = fallback;
    }

    @Override
    @Transactional(readOnly = true)
    public String complete(LlmRequest request) {
        Optional<LlmProviderUsage> active = usageRepository
                .findByPurposeAndActiveTrue(request.purpose());
        if (active.isEmpty()) {
            LOG.debug("No active LLM provider for purpose {} — using mock fallback.",
                    request.purpose());
            return fallback.complete(request);
        }
        LlmProvider provider = active.get().getProvider();
        return httpClient.complete(provider, resolveApiKey(provider), request);
    }

    private String resolveApiKey(LlmProvider provider) {
        String encrypted = provider.getApiKeyEncrypted();
        if (encrypted != null && !encrypted.isBlank()) {
            return secretCipher.decrypt(encrypted);
        }
        return secretStore.resolve(provider.getApiKeySecretRef());
    }
}
