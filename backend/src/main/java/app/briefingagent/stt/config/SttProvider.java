package app.briefingagent.stt.config;

import app.briefingagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "stt_provider")
public class SttProvider extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "endpoint_url", nullable = false, length = 500)
    private String endpointUrl;

    @Column(name = "model_name", nullable = false, length = 200)
    private String modelName;

    @Column(name = "api_key_secret_ref", length = 200)
    private String apiKeySecretRef;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", columnDefinition = "jsonb")
    private Map<String, Object> parameters;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "last_tested_at")
    private Instant lastTestedAt;

    @Column(name = "last_test_result", length = 20)
    private String lastTestResult;

    @Column(name = "last_test_message", columnDefinition = "text")
    private String lastTestMessage;

    protected SttProvider() {
    }

    public SttProvider(String name, String endpointUrl, String modelName) {
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

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
