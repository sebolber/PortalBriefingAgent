package app.briefingagent.retention;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "briefingagent.retention")
public class RetentionProperties {

    /** Default transcript retention horizon in months when no audience override applies. */
    @Min(1)
    private int transcriptMonths = 12;

    /** Default summary retention in months when an audience leaves the column NULL. */
    @Min(1)
    private int summaryDefaultMonths = 12;

    /** Author-deletion grace period (months) between deactivation and physical deletion. */
    @Min(1)
    private int authorDeletionMonths = 6;

    public int getTranscriptMonths() {
        return transcriptMonths;
    }

    public void setTranscriptMonths(int transcriptMonths) {
        this.transcriptMonths = transcriptMonths;
    }

    public int getSummaryDefaultMonths() {
        return summaryDefaultMonths;
    }

    public void setSummaryDefaultMonths(int summaryDefaultMonths) {
        this.summaryDefaultMonths = summaryDefaultMonths;
    }

    public int getAuthorDeletionMonths() {
        return authorDeletionMonths;
    }

    public void setAuthorDeletionMonths(int authorDeletionMonths) {
        this.authorDeletionMonths = authorDeletionMonths;
    }
}
