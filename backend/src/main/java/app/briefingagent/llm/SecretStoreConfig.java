package app.briefingagent.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Registers the default {@link SecretStore} implementation if the
 * application context does not already contain one. Phase 1 ships
 * {@link EnvSecretStore}; a Vault- or Azure-Key-Vault-backed bean can
 * replace it by being declared with {@code @Primary} or by being the
 * sole bean of type {@link SecretStore} on the classpath.
 *
 * <p>{@code @ConditionalOnMissingBean} is intentionally placed on the
 * {@code @Bean} method rather than on a component-scanned class: that
 * is where Spring Boot evaluates the condition reliably and avoids the
 * chicken-and-egg problem where a self-conditioned {@code @Component}
 * can exclude itself.</p>
 */
@Configuration(proxyBeanMethods = false)
public class SecretStoreConfig {

    @Bean
    @ConditionalOnMissingBean(SecretStore.class)
    public SecretStore secretStore(Environment environment) {
        return new EnvSecretStore(environment);
    }
}
