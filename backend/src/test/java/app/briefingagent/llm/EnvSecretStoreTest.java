package app.briefingagent.llm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class EnvSecretStoreTest {

    @Test
    void blank_or_null_ref_returns_empty_string() {
        EnvSecretStore store = new EnvSecretStore(new MockEnvironment());

        assertThat(store.resolve(null)).isEmpty();
        assertThat(store.resolve("")).isEmpty();
        assertThat(store.resolve("   ")).isEmpty();
    }

    @Test
    void resolves_via_environment_property_lookup() {
        MockEnvironment env = new MockEnvironment().withProperty("LLM_KEY", "shh");
        EnvSecretStore store = new EnvSecretStore(env);

        assertThat(store.resolve("LLM_KEY")).isEqualTo("shh");
    }

    @Test
    void unresolved_ref_returns_empty_string() {
        EnvSecretStore store = new EnvSecretStore(new MockEnvironment());

        assertThat(store.resolve("MISSING")).isEmpty();
    }
}
