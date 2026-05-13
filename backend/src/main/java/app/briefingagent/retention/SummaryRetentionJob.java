package app.briefingagent.retention;

import app.briefingagent.summary.Summary;
import app.briefingagent.summary.SummaryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes summaries past their per-audience retention horizon. A NULL
 * audience-level horizon means "unbegrenzt" per the spec; persons inherit
 * the global default. The pruning is conservative: anything within the
 * horizon stays, anything beyond is dropped via the repository.
 */
@Component
public class SummaryRetentionJob {

    private static final Logger LOG = LoggerFactory.getLogger(SummaryRetentionJob.class);

    private final SummaryRepository summaryRepository;
    private final RetentionProperties properties;
    private final Clock clock;

    public SummaryRetentionJob(SummaryRepository summaryRepository,
                               RetentionProperties properties,
                               Clock clock) {
        this.summaryRepository = summaryRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "0 15 2 * * *", zone = "UTC")
    @Transactional
    public int runOnce() {
        ZonedDateTime nowZ = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("UTC"));
        Instant earliestPossibleCutoff = nowZ
                .minus(Period.ofMonths(properties.getSummaryDefaultMonths()))
                .toInstant();
        List<Summary> candidates = summaryRepository.findByCreatedAtBefore(earliestPossibleCutoff);
        int deleted = 0;
        for (Summary s : candidates) {
            Integer months = retentionMonthsFor(s);
            if (months == null) {
                continue;
            }
            Instant audienceCutoff = nowZ.minus(Period.ofMonths(months)).toInstant();
            if (s.getCreatedAt().isBefore(audienceCutoff)) {
                summaryRepository.delete(s);
                deleted++;
            }
        }
        LOG.debug("SummaryRetentionJob deleted {} summaries (cutoff base {}).", deleted, nowZ);
        return deleted;
    }

    private Integer retentionMonthsFor(Summary summary) {
        return switch (summary.getAudienceType()) {
            case TOPIC -> summary.getAudienceTopic().getSummaryRetentionMonths() != null
                    ? summary.getAudienceTopic().getSummaryRetentionMonths()
                    : properties.getSummaryDefaultMonths();
            case PERSONGROUP -> summary.getAudiencePersonGroup().getSummaryRetentionMonths() != null
                    ? summary.getAudiencePersonGroup().getSummaryRetentionMonths()
                    : properties.getSummaryDefaultMonths();
            case PERSON -> properties.getSummaryDefaultMonths();
        };
    }

}
