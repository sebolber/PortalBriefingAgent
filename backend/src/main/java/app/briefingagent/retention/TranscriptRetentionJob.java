package app.briefingagent.retention;

import app.briefingagent.ereignis.Ereignis;
import app.briefingagent.ereignis.EreignisRepository;
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
 * Nullifies {@code Ereignis.transcript_text} once a transcript has aged past
 * the configured retention horizon. The audit trail (Summaries, Tasks)
 * stays intact, so the resulting tombstone is consistent with the spec's
 * data-minimisation policy.
 */
@Component
public class TranscriptRetentionJob {

    private static final Logger LOG = LoggerFactory.getLogger(TranscriptRetentionJob.class);

    private final EreignisRepository ereignisRepository;
    private final RetentionProperties properties;
    private final Clock clock;

    public TranscriptRetentionJob(EreignisRepository ereignisRepository,
                                  RetentionProperties properties,
                                  Clock clock) {
        this.ereignisRepository = ereignisRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    @Transactional
    public int runOnce() {
        Instant cutoff = ZonedDateTime.now(clock)
                .minus(Period.ofMonths(properties.getTranscriptMonths()))
                .withZoneSameInstant(ZoneId.of("UTC"))
                .toInstant();
        List<Ereignis> due = ereignisRepository
                .findByTranscriptTextIsNotNullAndCreatedAtBefore(cutoff);
        for (Ereignis e : due) {
            e.setTranscriptText(null);
            ereignisRepository.save(e);
        }
        LOG.debug("TranscriptRetentionJob blanked {} transcripts older than {}.", due.size(), cutoff);
        return due.size();
    }
}
