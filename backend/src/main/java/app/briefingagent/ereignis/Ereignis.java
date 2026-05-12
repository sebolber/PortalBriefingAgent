package app.briefingagent.ereignis;

import app.briefingagent.common.BaseEntity;
import app.briefingagent.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ereignis")
public class Ereignis extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;

    @Column(name = "source_type", nullable = false, length = 20)
    private EreignisSourceType sourceType;

    @Column(name = "transcript_text", columnDefinition = "text")
    private String transcriptText;

    @Column(name = "transcript_source", length = 20)
    private TranscriptSource transcriptSource;

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "character_count")
    private Integer characterCount;

    @Column(name = "truncated_at_limit", nullable = false)
    private boolean truncatedAtLimit;

    @Column(name = "review_status", nullable = false, length = 20)
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;

    @Column(name = "transcript_retention_until")
    private Instant transcriptRetentionUntil;

    protected Ereignis() {
    }

    public Ereignis(UserAccount author, EreignisSourceType sourceType) {
        this.author = author;
        this.sourceType = sourceType;
    }

    public UserAccount getAuthor() {
        return author;
    }

    public EreignisSourceType getSourceType() {
        return sourceType;
    }

    public String getTranscriptText() {
        return transcriptText;
    }

    public void setTranscriptText(String transcriptText) {
        this.transcriptText = transcriptText;
    }

    public TranscriptSource getTranscriptSource() {
        return transcriptSource;
    }

    public void setTranscriptSource(TranscriptSource transcriptSource) {
        this.transcriptSource = transcriptSource;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getCharacterCount() {
        return characterCount;
    }

    public void setCharacterCount(Integer characterCount) {
        this.characterCount = characterCount;
    }

    public boolean isTruncatedAtLimit() {
        return truncatedAtLimit;
    }

    public void setTruncatedAtLimit(boolean truncatedAtLimit) {
        this.truncatedAtLimit = truncatedAtLimit;
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(ReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public Instant getTranscriptRetentionUntil() {
        return transcriptRetentionUntil;
    }

    public void setTranscriptRetentionUntil(Instant transcriptRetentionUntil) {
        this.transcriptRetentionUntil = transcriptRetentionUntil;
    }
}
