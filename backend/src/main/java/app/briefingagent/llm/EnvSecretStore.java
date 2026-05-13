package app.briefingagent.llm;

import org.springframework.core.env.Environment;

/**
 * Default {@link SecretStore} implementation: a textual reference on a
 * provider row is interpreted as the name of an environment variable,
 * and the resolved value is whatever the JVM environment / Spring
 * property source has under that name. The class itself is not a
 * {@code @Component}; {@link SecretStoreConfig} wires it as the default
 * bean and lets a Vault-backed implementation register via the standard
 * {@code @Primary} / explicit {@code @Bean} override mechanism.
 */
public class EnvSecretStore implements SecretStore {

    private final Environment environment;

    public EnvSecretStore(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String resolve(String secretRef) {
        if (secretRef == null || secretRef.isBlank()) {
            return "";
        }
        String value = environment.getProperty(secretRef);
        return value == null ? "" : value;
    }
}
