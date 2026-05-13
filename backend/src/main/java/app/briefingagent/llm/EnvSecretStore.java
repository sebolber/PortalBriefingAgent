package app.briefingagent.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(SecretStore.class)
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
