package app.briefingagent.llm.config;

import app.briefingagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "llm_provider")
public class LlmProvider extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "endpoint_url", nullable = false, length = 500)
    private String endpointUrl;

    @Column(name = "model_name", nullable = false, length = 200)
    private String modelName;

    @Column(name = "api_key_secret_ref", length = 200)
    private String apiKeySecretRef;

    /**
     * Base64-encoded AES-256-GCM envelope ({@code nonce || ciphertext})
     * produced by {@code SecretCipher}. Never holds the plaintext key.
     * When present this column takes precedence over
     * {@link #apiKeySecretRef}.
     */
    @Column(name = "api_key_encrypted", columnDefinition = "text")
    private String apiKeyEncrypted;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", columnDefinition = "jsonb")
    private java.util.Map<String, Object> parameters;

    @Column(name = "api_type", nullable = false, length = 50)
    private String apiType = "openai_compatible";

    @Column(name = "last_tested_at")
    private Instant lastTestedAt;

    @Column(name = "last_test_result", length = 20)
    private String lastTestResult;

    @Column(name = "last_test_message", columnDefinition = "text")
    private String lastTestMessage;

    protected LlmProvider() {
    }

    public LlmProvider(String name, String endpointUrl, String modelName) {
        this.name = name;
        this.endpointUrl = endpointUrl;
        this.modelName = modelName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getApiKeySecretRef() {
        return apiKeySecretRef;
    }

    public void setApiKeySecretRef(String apiKeySecretRef) {
        this.apiKeySecretRef = apiKeySecretRef;
    }

    public String getApiKeyEncrypted() {
        return apiKeyEncrypted;
    }

    public void setApiKeyEncrypted(String apiKeyEncrypted) {
        this.apiKeyEncrypted = apiKeyEncrypted;
    }

    public java.util.Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(java.util.Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public String getApiType() {
        return apiType;
    }

    public void setApiType(String apiType) {
        this.apiType = apiType;
    }

    public Instant getLastTestedAt() {
        return lastTestedAt;
    }

    public void setLastTestedAt(Instant lastTestedAt) {
        this.lastTestedAt = lastTestedAt;
    }

    public String getLastTestResult() {
        return lastTestResult;
    }

    public void setLastTestResult(String lastTestResult) {
        this.lastTestResult = lastTestResult;
    }

    public String getLastTestMessage() {
        return lastTestMessage;
    }

    public void setLastTestMessage(String lastTestMessage) {
        this.lastTestMessage = lastTestMessage;
    }
}
