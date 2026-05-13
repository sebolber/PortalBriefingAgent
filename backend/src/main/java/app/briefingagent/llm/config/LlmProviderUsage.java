package app.briefingagent.llm.config;

import app.briefingagent.common.BaseEntity;
import app.briefingagent.llm.LlmPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "llm_provider_usage",
        uniqueConstraints = @UniqueConstraint(
                name = "llm_provider_usage_unique",
                columnNames = {"llm_provider_id", "purpose"}))
public class LlmProviderUsage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "llm_provider_id", nullable = false)
    private LlmProvider provider;

    @Column(name = "purpose", nullable = false, length = 50)
    private LlmPurpose purpose;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected LlmProviderUsage() {
    }

    public LlmProviderUsage(LlmProvider provider, LlmPurpose purpose, boolean active) {
        this.provider = provider;
        this.purpose = purpose;
        this.active = active;
    }

    public LlmProvider getProvider() {
        return provider;
    }

    public LlmPurpose getPurpose() {
        return purpose;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
