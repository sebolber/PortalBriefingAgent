package app.briefingagent.prompt;

import app.briefingagent.common.AuditedEntity;
import app.briefingagent.llm.LlmPurpose;
import app.briefingagent.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "prompt_template")
public class PromptTemplate extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;

    @Column(name = "purpose", nullable = false, length = 50)
    private LlmPurpose purpose;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "active", nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_author_id", nullable = false)
    private UserAccount createdByAuthor;

    protected PromptTemplate() {
    }

    public PromptTemplate(UserAccount author, LlmPurpose purpose, String content,
                          int version, boolean active, UserAccount createdByAuthor) {
        this.author = author;
        this.purpose = purpose;
        this.content = content;
        this.version = version;
        this.active = active;
        this.createdByAuthor = createdByAuthor;
    }

    public UserAccount getAuthor() {
        return author;
    }

    public LlmPurpose getPurpose() {
        return purpose;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getVersion() {
        return version;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UserAccount getCreatedByAuthor() {
        return createdByAuthor;
    }
}
